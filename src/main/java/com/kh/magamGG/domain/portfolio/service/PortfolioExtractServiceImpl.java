package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.dto.PortfolioExtractDto;
import com.kh.magamGG.global.storage.FileStorageService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 포트폴리오 이미지 → Spring AI Vision → 구조화 추출 (명함 서비스와 동일 패턴)
 * - 이미지 파일 / 웹 페이지 URL(스크린샷) 지원
 */
@Service
@Slf4j
public class PortfolioExtractServiceImpl implements PortfolioExtractService {

    private static final String EXTRACT_PROMPT = """
        You are a structured data assistant. The user has uploaded a screenshot of a portfolio, resume, or profile page. Convert it into one JSON object only. Do not refuse.

        CRITICAL - USE ONLY THE IMAGE: Extract ONLY what is actually visible in the image. Do NOT invent names (e.g. never use "John Doe"), companies ("Tech Company"), projects ("Project A/B/C"), emails (johndoe@example.com), or phone numbers. If the image shows "새삐Sap" then name is "새삐Sap". If something is not in the image, use "" or [].

        CRITICAL - careerItems: The image may be a long full-page screenshot. You MUST read the ENTIRE image from top to bottom. Put EVERY line in the career section as a separate string in careerItems. Put the date/period at the BEGINNING of each string when present (e.g. "2012년 - 2016년 IMC games / 사원, 서울", "2017 P 사 / C 프로젝트 사원"). Do NOT stop after the first company. Same for projects: each element with date/period at the front when present (e.g. "2018~ L사 D프로젝트, 블랙시타델", "~2022 T사 B 프로젝트").

        Rules:
        - Output ONLY one JSON object. No explanation, no markdown, no code fence. Start with { and end with }.
        - name, role, email, phone, projects, careerItems, workStyle, skills: ONLY from the image. Missing = "" or [].
        - Keys only: name, role, email, phone, projects, careerItems, career, workStyle, skills.

        Example shape (use empty when not in image): {"name":"","role":"","email":"","phone":"","projects":[],"careerItems":[],"career":"","workStyle":[],"skills":[]}

        Output the COMPLETE JSON. Include ALL careerItems from the image. Nothing but this JSON.
        """;

    /** 청크(페이지 일부)용 프롬프트: 이 조각에 보이는 모든 줄을 나열하라, 한 줄로 합치지 말 것. */
    private static final String CHUNK_EXTRACT_PROMPT = """
        This image is ONE FRAGMENT of a long portfolio page. Extract ONLY what is actually visible in THIS image. Do NOT invent or use placeholder data (no "John Doe", "Tech Company", "Project A", etc.). Not in the image = "" or [].

        CRITICAL: In careerItems put EVERY line—one string per line. Put date/period at the BEGINNING of each item when present (e.g. "2012년 - 2016년 IMC games / 사원", "2017 원화, UI 아이콘 제작"). Same for projects: date/period first when present (e.g. "2018~ 블랙시타델"). Do NOT merge lines.

        You MUST respond with exactly one JSON object. Use ONLY these English keys: name, role, email, phone, projects, careerItems, career, workStyle, skills. Use "" or [] for missing. No markdown, no code fence, no explanation. Start with { and end with }.
        """;

    private static final String EXTRACT_FROM_PAGE_PROMPT = """
        You are a structured data assistant. The text below was taken from a portfolio or resume page. Convert it into a single JSON object. Output only the JSON, no explanation or markdown.

        CRITICAL - USE ONLY THE TEXT BELOW: Extract ONLY what actually appears in the provided text. Do NOT invent, fabricate, or use example/placeholder data. Never output "John Doe", "Software Engineer", "Tech Company", "Project A/B/C", "johndoe@example.com", "123-456-7890" or any generic placeholder. If the text says "새삐Sap" then name is "새삐Sap". If the text has no email, use "". If something is not in the text, use "" or [].

        CRITICAL - careerItems: Include EVERY line as a separate string in careerItems. Put the date/period at the BEGINNING of each string when present (e.g. "2012년 - 2016년 IMC games / 사원, 서울", "2017 P 사 / C 프로젝트"). Do NOT stop after the first company. Same for projects: date/period first when present (e.g. "2018~ 블랙시타델", "2019~2020 A사 G프로젝트"). Do not merge or truncate.

        Keys only: name, role, email, phone, projects (array), careerItems (array), career (empty string), workStyle (array), skills (array). Missing = "" or []. Output the COMPLETE JSON. Nothing but the JSON object.
        """;

    /** 섹션 단위 추출: 상단(이름·직무·연락처) 전용 */
    private static final String SECTION_HEADER_PROMPT = """
        The text below is the header/intro of a portfolio (name, role, contact). Extract ONLY what appears in the text. Do NOT invent (no "John Doe", "johndoe@example.com"). Output JSON only: {"name":"","role":"","email":"","phone":""}. Missing = "".
        """;
    /** 섹션 단위 추출: 경력 블록만 → careerItems 한 줄씩 전부, 연도·기간은 앞에 배치 */
    private static final String SECTION_CAREER_PROMPT = """
        The text below is ONLY the career section of a portfolio. List EVERY line as a separate element in careerItems. Put the date/period at the BEGINNING of each string when present (e.g. "2012년 - 2016년 IMC games / 사원, 서울", "2017 P 사 사원"). Do NOT invent. Do NOT skip lines. Output JSON only: {"careerItems":[]}. Nothing else.
        """;
    /** 섹션 단위 추출: 프로젝트·작업스타일·기술 (프로젝트는 참여 기간을 앞에 배치) */
    private static final String SECTION_PROJECTS_REST_PROMPT = """
        The text below is from portfolio sections (projects, work style, skills). Extract ONLY what appears. For projects: put participation period/dates at the BEGINNING of each string when present (e.g. "2018~ 블랙시타델", "2019~2020 A사 G프로젝트"). Do NOT invent. Output JSON only: {"projects":[],"workStyle":[],"skills":[]}. Missing = [].
        """;

    /** 섹션 구분용 키워드 (긴 것 우선). 해당 줄부터 다음 섹션 전까지가 해당 영역. */
    private static final String[][] SECTION_MARKERS = {
            {"참여 프로젝트", "projects"},
            {"경력 사항", "career"},
            {"경력", "career"},
            {"작업 스타일", "workStyle"},
            {"사용 기술", "skills"},
            {"프로젝트", "projects"},
            {"기술", "skills"},
            {"스킬", "skills"},
    };

    /** 파싱된 페이지 섹션: 헤더(이름·직무 등), 경력, 참여 프로젝트, 작업 스타일, 사용 기술 */
    private record PageSections(String header, String career, String projects, String workStyle, String skills) {
        boolean hasCareer() { return career != null && !career.isBlank(); }
        boolean hasProjects() { return projects != null && !projects.isBlank(); }
    }

    /** 본문 텍스트를 섹션별로 분리 (경력·참여 프로젝트 등 제목 기준). 구분 실패 시 null. */
    private PageSections parseSections(String fullText) {
        if (fullText == null || fullText.isBlank()) return null;
        String[] lines = fullText.split("\n", -1);
        Map<Integer, String> sectionStarts = new LinkedHashMap<>();
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty()) continue;
            for (String[] marker : SECTION_MARKERS) {
                String keyword = marker[0];
                String key = marker[1];
                if (trimmed.equals(keyword) || trimmed.startsWith(keyword + " ") || trimmed.startsWith(keyword + ":")) {
                    if (!sectionStarts.containsValue(key)) {
                        sectionStarts.put(i, key);
                    }
                    break;
                }
            }
        }
        if (sectionStarts.isEmpty()) return null;
        List<Integer> indices = new ArrayList<>(sectionStarts.keySet());
        indices.sort(Integer::compareTo);
        int firstSectionLine = indices.get(0);
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < firstSectionLine; i++) {
            if (header.length() > 0) header.append("\n");
            header.append(lines[i]);
        }
        String career = null, projects = null, workStyle = null, skills = null;
        for (int s = 0; s < indices.size(); s++) {
            int start = indices.get(s);
            int end = s + 1 < indices.size() ? indices.get(s + 1) : lines.length;
            StringBuilder block = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (block.length() > 0) block.append("\n");
                block.append(lines[i]);
            }
            String key = sectionStarts.get(start);
            switch (key) {
                case "career" -> career = block.toString().trim();
                case "projects" -> projects = (projects != null ? projects + "\n" : "") + block.toString().trim();
                case "workStyle" -> workStyle = block.toString().trim();
                case "skills" -> skills = block.toString().trim();
                default -> {}
            }
        }
        return new PageSections(header.toString().trim(), career, projects, workStyle, skills);
    }

    private static final int MIN_PAGE_TEXT_LENGTH = 50;
    private static final int MIN_TEXT_FOR_LLM = 150;
    private static final int SCREENSHOT_MAX_WIDTH = 1280;
    private static final int CHUNK_MAX_HEIGHT = 1400;
    private static final int CHUNK_OVERLAP = 150;

    private final Optional<ChatClient.Builder> chatClientBuilder;
    private final FileStorageService fileStorageService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public PortfolioExtractServiceImpl(
            @org.springframework.beans.factory.annotation.Autowired(required = false) ChatClient.Builder chatClientBuilder,
            FileStorageService fileStorageService) {
        this.chatClientBuilder = Optional.ofNullable(chatClientBuilder);
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PortfolioExtractDto extractFromImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("포트폴리오 이미지 파일이 필요합니다.");
        }
        String savedFileName = fileStorageService.saveFile(imageFile);
        Path imagePath = Paths.get(uploadDir).resolve(savedFileName);
        try {
            Resource imageResource = new FileSystemResource(imagePath.toFile());
            return extractFromImageResource(imageResource);
        } finally {
            try {
                if (imagePath.toFile().exists()) {
                    imagePath.toFile().delete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public PortfolioExtractDto extractFromPageUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) {
            throw new IllegalArgumentException("페이지 URL이 필요합니다.");
        }
        String html = fetchHtml(pageUrl.trim());
        String bodyText = extractBodyText(html);
        if (bodyText == null || bodyText.length() < MIN_PAGE_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "페이지에서 추출한 텍스트가 없거나 너무 짧습니다. " +
                    "JavaScript로 렌더링되는 페이지는 스크린샷 이미지를 업로드해 주세요.");
        }
        return extractFromTextWithLlm(bodyText);
    }

    @Override
    public PortfolioExtractDto extractFromPageScreenshot(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) {
            throw new IllegalArgumentException("페이지 URL이 필요합니다.");
        }
        URI uri = URI.create(pageUrl.trim());
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("http 또는 https URL만 지원합니다.");
        }
        String url = pageUrl.trim();
        String pageText = fetchPageTextWithPlaywright(url);
        if (pageText != null && pageText.trim().length() >= MIN_TEXT_FOR_LLM) {
            try {
                log.info("페이지 텍스트 추출 성공 ({}자), 텍스트 기반 LLM 추출 사용", pageText.trim().length());
                return extractFromTextWithLlm(pageText.trim());
            } catch (Exception e) {
                log.warn("텍스트 기반 추출 실패, 스크린샷 폴백: {}", e.getMessage());
            }
        }
        log.info("스크린샷+Vision 폴백 (텍스트 {}자)", pageText != null ? pageText.length() : 0);
        byte[] screenshotBytes = captureFullPageScreenshot(url);
        if (screenshotBytes == null || screenshotBytes.length == 0) {
            throw new RuntimeException("페이지 스크린샷을 찍지 못했습니다.");
        }
        byte[] imageBytes = resizeImageIfNeeded(screenshotBytes, SCREENSHOT_MAX_WIDTH);
        List<byte[]> chunks = splitImageIntoVerticalChunks(imageBytes, CHUNK_MAX_HEIGHT, CHUNK_OVERLAP);
        if (chunks.isEmpty()) {
            throw new RuntimeException("스크린샷을 처리할 수 없습니다.");
        }
        List<PortfolioExtractDto> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            PortfolioExtractDto dto = extractFromImageResourceQuiet(new ByteArrayResource(chunks.get(i)), MimeTypeUtils.IMAGE_PNG_VALUE, CHUNK_EXTRACT_PROMPT);
            if (dto != null) {
                partials.add(dto);
                int n = dto.careerItems() != null ? dto.careerItems().size() : 0;
                log.info("청크 {}/{} 추출됨: careerItems {}개", i + 1, chunks.size(), n);
            } else {
                log.warn("청크 {}/{} 추출 실패 또는 빈 결과", i + 1, chunks.size());
            }
        }
        if (partials.isEmpty()) {
            log.info("청크 추출 전부 실패 → 전체 스크린샷 1장으로 Vision 재시도");
            PortfolioExtractDto fullResult = extractFromImageResourceQuiet(
                    new ByteArrayResource(imageBytes), MimeTypeUtils.IMAGE_PNG_VALUE, EXTRACT_PROMPT);
            if (fullResult != null && !isEmpty(fullResult)) {
                log.info("전체 이미지 1장 추출 성공");
                return fullResult;
            }
            throw new com.kh.magamGG.global.exception.PortfolioExtractException(
                    "이미지에서 포트폴리오 정보를 추출하지 못했습니다. " +
                    "페이지가 로그인/접근 제한이 있거나, AI가 내용을 인식하지 못했을 수 있습니다. " +
                    "해당 페이지를 캡처한 이미지 파일을 직접 업로드(이미지 파일 선택)해 보세요.");
        }
        return mergeExtractResults(partials);
    }

    private String fetchPageTextWithPlaywright(String pageUrl) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setArgs(List.of("--disable-http2")));
            try (BrowserContext context = browser.newContext()) {
                Page page = context.newPage();
                page.setDefaultNavigationTimeout(30_000);
                page.setDefaultTimeout(20_000);
                page.navigate(pageUrl);
                page.waitForLoadState(LoadState.LOAD);
                try {
                    Thread.sleep(4_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                try {
                    Object heightObj = page.evaluate("() => document.body.scrollHeight");
                    long totalHeight = heightObj instanceof Number ? ((Number) heightObj).longValue() : 0L;
                    if (totalHeight > 0) {
                        long step = 800;
                        for (long y = 0; y < totalHeight; y += step) {
                            page.evaluate("(y) => window.scrollTo(0, y)", y);
                            Thread.sleep(150);
                        }
                        page.evaluate("() => window.scrollTo(0, 0)");
                        Thread.sleep(1_000);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                } catch (Exception e) {
                    log.debug("스크롤 스킵: {}", e.getMessage());
                }
                Object textObj = page.evaluate("() => { const el = document.querySelector('main') || document.querySelector('article') || document.querySelector('[role=\"main\"]') || document.body; return el ? el.innerText : ''; }");
                String text = textObj != null ? textObj.toString().trim() : "";
                return text.isEmpty() ? null : text;
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            log.warn("Playwright 텍스트 추출 실패: {} - {}", pageUrl, e.getMessage());
            return null;
        }
    }

    private byte[] captureFullPageScreenshot(String pageUrl) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setArgs(List.of("--disable-http2")));
            try (BrowserContext context = browser.newContext()) {
                Page page = context.newPage();
                page.setDefaultNavigationTimeout(30_000);
                page.setDefaultTimeout(20_000);
                page.navigate(pageUrl);
                page.waitForLoadState(LoadState.LOAD);
                try {
                    Thread.sleep(4_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("스크린샷 대기가 중단되었습니다.", ie);
                }
                try {
                    Object heightObj = page.evaluate("() => document.body.scrollHeight");
                    long totalHeight = heightObj instanceof Number ? ((Number) heightObj).longValue() : 0L;
                    if (totalHeight > 0) {
                        long step = 800;
                        for (long y = 0; y < totalHeight; y += step) {
                            page.evaluate("(y) => window.scrollTo(0, y)", y);
                            Thread.sleep(150);
                        }
                        page.evaluate("() => window.scrollTo(0, 0)");
                        Thread.sleep(1_000);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("스크롤 대기가 중단되었습니다.", ie);
                } catch (Exception e) {
                    log.debug("스크롤 트리거 스킵: {}", e.getMessage());
                }
                return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            log.warn("Playwright 스크린샷 실패: {} - {}", pageUrl, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Executable") || msg.contains("doesn't exist") || msg.contains("Browser executable") || msg.contains("not found")) {
                throw new RuntimeException(
                        "Playwright 브라우저가 설치되지 않았습니다. 서버에서 다음을 실행하세요: npx playwright install chromium (또는 java -cp ... com.microsoft.playwright.CLI install)");
            }
            if (msg.contains("ERR_HTTP2_PROTOCOL_ERROR") || msg.contains("RST_STREAM")) {
                throw new RuntimeException(
                        "이 URL은 서버에서 접근을 제한하고 있습니다. 이미지 파일을 직접 업로드해 주세요. (파일 선택 → 이미지 업로드)");
            }
            throw new RuntimeException("페이지 스크린샷에 실패했습니다: " + msg, e);
        }
    }

    private List<byte[]> splitImageIntoVerticalChunks(byte[] imageBytes, int maxChunkHeight, int overlap) {
        List<byte[]> result = new ArrayList<>();
        if (imageBytes == null || imageBytes.length == 0) return result;
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                result.add(imageBytes);
                return result;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            if (h <= maxChunkHeight) {
                result.add(imageBytes);
                return result;
            }
            int step = maxChunkHeight - overlap;
            for (int y = 0; y < h; y += step) {
                int chunkHeight = Math.min(maxChunkHeight, h - y);
                if (chunkHeight <= 0) break;
                BufferedImage chunk = img.getSubimage(0, y, w, chunkHeight);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(chunk, "png", out);
                result.add(out.toByteArray());
                if (y + chunkHeight >= h) break;
            }
            log.info("스크린샷 분할: 높이 {}px -> {}개 청크", h, result.size());
        } catch (Exception e) {
            log.warn("스크린샷 분할 실패, 원본 1장 사용: {}", e.getMessage());
            result.add(imageBytes);
        }
        return result;
    }

    private PortfolioExtractDto mergeExtractResults(List<PortfolioExtractDto> partials) {
        if (partials == null || partials.isEmpty()) return null;
        String name = null, role = null, email = null, phone = null, career = "";
        LinkedHashSet<String> careerSet = new LinkedHashSet<>();
        LinkedHashSet<String> projectSet = new LinkedHashSet<>();
        LinkedHashSet<String> workStyleSet = new LinkedHashSet<>();
        LinkedHashSet<String> skillsSet = new LinkedHashSet<>();
        for (PortfolioExtractDto d : partials) {
            if (d == null) continue;
            if (name == null && d.name() != null && !d.name().isBlank()) name = d.name();
            if (role == null && d.role() != null && !d.role().isBlank()) role = d.role();
            if (email == null && d.email() != null && !d.email().isBlank()) email = d.email();
            if (phone == null && d.phone() != null && !d.phone().isBlank()) phone = d.phone();
            if (d.careerItems() != null) for (String s : d.careerItems()) if (s != null && !s.isBlank()) careerSet.add(s.trim());
            if (d.projects() != null) for (String s : d.projects()) if (s != null && !s.isBlank()) projectSet.add(s.trim());
            if (d.workStyle() != null) for (String s : d.workStyle()) if (s != null && !s.isBlank()) workStyleSet.add(s.trim());
            if (d.skills() != null) for (String s : d.skills()) if (s != null && !s.isBlank()) skillsSet.add(s.trim());
        }
        List<String> careerItems = new ArrayList<>(careerSet);
        List<String> projects = new ArrayList<>(projectSet);
        List<String> workStyle = new ArrayList<>(workStyleSet);
        List<String> skills = new ArrayList<>(skillsSet);
        if (name == null) name = "";
        if (role == null) role = "";
        if (email == null) email = "";
        if (phone == null) phone = "";
        if (partials.size() > 1 && careerItems.size() <= 2) {
            log.warn("병합 후 경력 항목이 {}개뿐입니다. 청크 {}개 중 추출 품질이 낮을 수 있습니다.", careerItems.size(), partials.size());
        }
        return new PortfolioExtractDto(name, role, email, phone, projects, careerItems, career, workStyle, skills);
    }

    private PortfolioExtractDto extractFromImageResourceQuiet(Resource imageResource, String mediaType, String promptText) {
        if (chatClientBuilder.isEmpty() || imageResource == null || !imageResource.exists()) return null;
        try {
            var chatClient = chatClientBuilder.get().build();
            org.springframework.util.MimeType mimeType = org.springframework.util.MimeTypeUtils.parseMimeType(mediaType);
            String response = chatClient.prompt()
                    .system(s -> s.text("You are a data extraction assistant. Convert the image to JSON only. Do not refuse. Put EVERY career line as a separate element in careerItems; put date/period at the BEGINNING of each string when present. Same for projects: date/period first. Do not merge into one line."))
                    .user(u -> u.text(promptText).media(mimeType, imageResource))
                    .call()
                    .content();
            PortfolioExtractDto parsed = parseExtractResponse(response);
            if (parsed == null && response != null && !response.isBlank()) {
                log.warn("Vision 응답 파싱 실패. 응답 앞 500자: {}", response.length() > 500 ? response.substring(0, 500) + "..." : response);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("청크 추출 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("청크 추출 예외 상세", e);
            }
            return null;
        }
    }

    private byte[] resizeImageIfNeeded(byte[] imageBytes, int maxWidth) {
        if (imageBytes == null || imageBytes.length == 0) return imageBytes;
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return imageBytes;
            int w = img.getWidth();
            if (w <= maxWidth) return imageBytes;
            int h = (int) (img.getHeight() * (double) maxWidth / w);
            Image scaled = img.getScaledInstance(maxWidth, h, Image.SCALE_SMOOTH);
            BufferedImage outImg = new BufferedImage(maxWidth, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImg.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            ImageIO.write(outImg, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("이미지 리사이즈 스킵: {}", e.getMessage());
            return imageBytes;
        }
    }

    private String fetchHtml(String pageUrl) {
        try {
            URI uri = URI.create(pageUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("http 또는 https URL만 지원합니다.");
            }
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("페이지를 가져올 수 없습니다. (HTTP " + response.statusCode() + ")");
            }
            return response.body();
        } catch (Exception e) {
            log.warn("페이지 HTML 다운로드 실패: {} - {}", pageUrl, e.getMessage());
            throw new IllegalArgumentException("페이지를 가져올 수 없습니다: " + e.getMessage(), e);
        }
    }

    private String extractBodyText(String html) {
        if (html == null || html.isBlank()) return "";
        try {
            Document doc = Jsoup.parse(html);
            var main = doc.selectFirst("main, article, [role='main']");
            String text = main != null ? main.text() : doc.body().text();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            log.warn("HTML 본문 추출 실패: {}", e.getMessage());
            return "";
        }
    }

    private PortfolioExtractDto extractFromTextWithLlm(String bodyText) {
        if (chatClientBuilder.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI API 키가 설정되지 않았습니다. 포트폴리오 인식을 사용하려면 " +
                    "OPENAI_API_KEY 환경 변수를 설정하세요.");
        }
        String text = bodyText != null ? bodyText.trim() : "";
        if (text.isBlank()) {
            throw new RuntimeException("추출할 본문 텍스트가 비어 있습니다.");
        }
        try {
            PageSections sections = parseSections(text);
            if (sections != null && (sections.hasCareer() || sections.hasProjects())) {
                log.info("섹션 단위 추출 사용: 경력={}, 프로젝트={}", sections.hasCareer(), sections.hasProjects());
                return extractFromSectionsAndMerge(sections, text);
            }
            var chatClient = chatClientBuilder.get().build();
            String prompt = EXTRACT_FROM_PAGE_PROMPT + "\n\n---\n" + truncateForPrompt(text);
            String response = chatClient.prompt()
                    .user(u -> u.text(prompt))
                    .call()
                    .content();
            PortfolioExtractDto parsed = parseExtractResponse(response);
            if (parsed == null || isEmpty(parsed)) {
                throw new RuntimeException(parsed == null
                        ? "추출 결과를 파싱할 수 없습니다. LLM이 JSON 형식이 아닌 응답을 반환했을 수 있습니다."
                        : "추출된 내용이 없습니다. 페이지에서 포트폴리오 정보를 찾지 못했을 수 있습니다.");
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Spring AI 텍스트 구조화 실패: {}", e.getMessage());
            throw new RuntimeException("포트폴리오 텍스트 구조화에 실패했습니다: " + e.getMessage());
        }
    }

    private PortfolioExtractDto extractFromSectionsAndMerge(PageSections sections, String fullText) {
        var chatClient = chatClientBuilder.get().build();
        String headerText = sections.header() != null && !sections.header().isBlank()
                ? truncateForPrompt(sections.header())
                : truncateForPrompt(fullText.substring(0, Math.min(3000, fullText.length())));
        PortfolioExtractDto headerDto = callSectionLlm(chatClient, SECTION_HEADER_PROMPT, headerText, "header");
        PortfolioExtractDto careerDto = null;
        if (sections.hasCareer()) {
            careerDto = callSectionLlm(chatClient, SECTION_CAREER_PROMPT, truncateForPrompt(sections.career()), "career");
        }
        String restText = "";
        if (sections.projects() != null && !sections.projects().isBlank()) restText += sections.projects() + "\n";
        if (sections.workStyle() != null && !sections.workStyle().isBlank()) restText += sections.workStyle() + "\n";
        if (sections.skills() != null && !sections.skills().isBlank()) restText += sections.skills();
        PortfolioExtractDto restDto = restText.isBlank() ? null : callSectionLlm(chatClient, SECTION_PROJECTS_REST_PROMPT, truncateForPrompt(restText), "projects/rest");
        return mergePartialDtos(headerDto, careerDto, restDto);
    }

    private PortfolioExtractDto callSectionLlm(ChatClient chatClient, String promptTemplate, String sectionText, String logLabel) {
        try {
            String prompt = promptTemplate + "\n\n---\n" + sectionText;
            String response = chatClient.prompt().user(u -> u.text(prompt)).call().content();
            PortfolioExtractDto dto = parseExtractResponse(response);
            if (dto != null && logLabel.equals("career") && dto.careerItems() != null) {
                log.info("섹션 추출 {}: careerItems {}개", logLabel, dto.careerItems().size());
            }
            return dto;
        } catch (Exception e) {
            log.warn("섹션 추출 실패 ({}): {}", logLabel, e.getMessage());
            return null;
        }
    }

    private PortfolioExtractDto mergePartialDtos(PortfolioExtractDto header, PortfolioExtractDto career, PortfolioExtractDto rest) {
        String name = firstNonBlank(header != null ? header.name() : null, rest != null ? rest.name() : null);
        String role = firstNonBlank(header != null ? header.role() : null, rest != null ? rest.role() : null);
        String email = firstNonBlank(header != null ? header.email() : null, rest != null ? rest.email() : null);
        String phone = firstNonBlank(header != null ? header.phone() : null, rest != null ? rest.phone() : null);
        List<String> careerItems = mergeLists(career != null ? career.careerItems() : null, rest != null ? rest.careerItems() : null);
        List<String> projects = mergeLists(header != null ? header.projects() : null, rest != null ? rest.projects() : null);
        List<String> workStyle = mergeLists(header != null ? header.workStyle() : null, rest != null ? rest.workStyle() : null);
        List<String> skills = mergeLists(header != null ? header.skills() : null, rest != null ? rest.skills() : null);
        if (name == null) name = "";
        if (role == null) role = "";
        if (email == null) email = "";
        if (phone == null) phone = "";
        return new PortfolioExtractDto(name, role, email, phone, projects, careerItems, "", workStyle, skills);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static List<String> mergeLists(List<String> a, List<String> b) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) for (String s : a) if (s != null && !s.isBlank()) set.add(s.trim());
        if (b != null) for (String s : b) if (s != null && !s.isBlank()) set.add(s.trim());
        return new ArrayList<>(set);
    }

    private static final int MAX_BODY_TEXT_FOR_LLM = 40_000;

    private String truncateForPrompt(String text) {
        if (text == null) return "";
        return text.length() <= MAX_BODY_TEXT_FOR_LLM ? text : text.substring(0, MAX_BODY_TEXT_FOR_LLM) + "\n...(이하 생략)";
    }

    private PortfolioExtractDto extractFromImageResource(Resource imageResource) {
        return extractFromImageResource(imageResource, MimeTypeUtils.IMAGE_JPEG_VALUE);
    }

    private PortfolioExtractDto extractFromImageResource(Resource imageResource, String mediaType) {
        if (chatClientBuilder.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI API 키가 설정되지 않았습니다. 포트폴리오 인식을 사용하려면 " +
                    "OPENAI_API_KEY 환경 변수를 설정하세요.");
        }
        if (!imageResource.exists()) {
            throw new IllegalArgumentException("이미지 파일을 찾을 수 없습니다.");
        }
        try {
            var chatClient = chatClientBuilder.get().build();
            org.springframework.util.MimeType mimeType = org.springframework.util.MimeTypeUtils.parseMimeType(mediaType);
            String response = chatClient.prompt()
                    .system(s -> s.text("You are a data extraction assistant. You convert portfolio/resume screenshots into JSON. The user provided the image for this. Respond with JSON only; do not refuse. For careerItems: include EVERY line as a separate array element; put the date/period at the BEGINNING of each string when present (e.g. '2012년 - 2016년 IMC games / 사원, 서울'). For projects: put participation period at the BEGINNING when present (e.g. '2018~ L사 D프로젝트', '~2022 T사'). Do not output only the first career line."))
                    .user(u -> u.text(EXTRACT_PROMPT).media(mimeType, imageResource))
                    .call()
                    .content();
            PortfolioExtractDto parsed = parseExtractResponse(response);
            if (parsed == null || isEmpty(parsed)) {
                throw new RuntimeException(parsed == null
                        ? "추출 결과를 파싱할 수 없습니다. 이미지에서 JSON을 생성하지 못했을 수 있습니다. 응답 형식을 확인해 주세요."
                        : "추출된 내용이 없습니다. 이미지에 포트폴리오/이력 정보가 보이는지 확인해 주세요.");
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Spring AI Vision 포트폴리오 추출 실패: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("401") || msg.contains("api key") || msg.contains("incorrect api key") || msg.contains("unauthorized") || msg.contains("invalid_api_key")) {
                throw new RuntimeException("OpenAI API 키가 없거나 잘못되었습니다. 서버 실행 시 환경 변수 OPENAI_API_KEY 를 설정하세요.");
            }
            throw new RuntimeException("포트폴리오 이미지 인식에 실패했습니다. OpenAI API 키를 확인하거나 이미지를 점검해주세요. (" + e.getMessage() + ")");
        }
    }

    private static boolean isEmpty(PortfolioExtractDto dto) {
        if (dto == null) return true;
        boolean hasName = dto.name() != null && !dto.name().isBlank();
        boolean hasRole = dto.role() != null && !dto.role().isBlank();
        boolean hasEmail = dto.email() != null && !dto.email().isBlank();
        boolean hasPhone = dto.phone() != null && !dto.phone().isBlank();
        boolean hasCareer = dto.career() != null && !dto.career().isBlank();
        boolean hasProjects = dto.projects() != null && !dto.projects().isEmpty();
        boolean hasCareerItems = dto.careerItems() != null && !dto.careerItems().isEmpty();
        boolean hasWorkStyle = dto.workStyle() != null && !dto.workStyle().isEmpty();
        boolean hasSkills = dto.skills() != null && !dto.skills().isEmpty();
        return !hasName && !hasRole && !hasEmail && !hasPhone && !hasCareer && !hasProjects && !hasCareerItems && !hasWorkStyle && !hasSkills;
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    private PortfolioExtractDto parseExtractResponse(String response) {
        if (response == null || response.isBlank()) {
            log.warn("포트폴리오 추출: LLM 응답이 비어 있음");
            return null;
        }
        String json = response.trim();
        if (json.startsWith("```")) {
            int firstNewline = json.indexOf('\n');
            int endBlock = json.indexOf("```", 3);
            if (firstNewline >= 0 && endBlock > firstNewline) {
                json = json.substring(firstNewline + 1, endBlock).trim();
            } else if (firstNewline >= 0) {
                json = json.substring(firstNewline + 1).trim();
            }
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return OBJECT_MAPPER.readValue(json, PortfolioExtractDto.class);
        } catch (Exception e) {
            String normalized = normalizeJsonKeysToEnglish(json);
            if (!normalized.equals(json)) {
                try {
                    return OBJECT_MAPPER.readValue(normalized, PortfolioExtractDto.class);
                } catch (Exception e2) {
                    log.debug("한글 키 치환 후 파싱 재시도 실패: {}", e2.getMessage());
                }
            }
            String snippet = json.length() > 600 ? json.substring(0, 600) + "..." : json;
            log.warn("포트폴리오 추출 JSON 파싱 실패. 응답 길이={}, 파싱 예외={}, 시도한 JSON 앞부분: {}", response.length(), e.getMessage(), snippet);
            return null;
        }
    }

    /** LLM이 한글 키로 응답한 JSON 문자열을 영문 키로 치환 (파싱 폴백) */
    private static String normalizeJsonKeysToEnglish(String json) {
        if (json == null || json.isBlank()) return json;
        String s = json;
        s = replaceJsonKey(s, "이름", "name");
        s = replaceJsonKey(s, "직무", "role");
        s = replaceJsonKey(s, "직위", "role");
        s = replaceJsonKey(s, "이메일", "email");
        s = replaceJsonKey(s, "메일", "email");
        s = replaceJsonKey(s, "전화", "phone");
        s = replaceJsonKey(s, "전화번호", "phone");
        s = replaceJsonKey(s, "연락처", "phone");
        s = replaceJsonKey(s, "프로젝트", "projects");
        s = replaceJsonKey(s, "경력항목", "careerItems");
        s = replaceJsonKey(s, "경력", "careerItems");
        s = replaceJsonKey(s, "작업스타일", "workStyle");
        s = replaceJsonKey(s, "작업 스타일", "workStyle");
        s = replaceJsonKey(s, "스킬", "skills");
        s = replaceJsonKey(s, "기술", "skills");
        return s;
    }

    private static String replaceJsonKey(String json, String keyKr, String keyEn) {
        return json.replace("\"" + keyKr + "\"", "\"" + keyEn + "\"");
    }
}
