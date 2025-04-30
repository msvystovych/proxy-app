package org.company.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlModifierServiceTest {

    private HtmlModifierService htmlModifierService;

    @BeforeEach
    void setUp() {
        htmlModifierService = new HtmlModifierService();
    }

    @Test
    void addsTrademarkToSingleSixLetterWord() {
        String input = "<html><body>Hello cherry world</body></html>";
        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("cherry™");
        assertThat(result).doesNotContain("Hello™"); // not 6 letters
    }

    @Test
    void addsTrademarkToMultipleSixLetterWords() {
        String input = "<html><body>banana cherry orange</body></html>";
        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("banana™");
        assertThat(result).contains("cherry™");
        assertThat(result).contains("orange™");
    }

    @Test
    void doesNotModifyNonTextElements() {
        String input = """
            <html><head><style>.class { color: red; }</style></head>
            <body><script>var banana = 123456;</script>banana</body></html>
            """;

        String result = htmlModifierService.modifyHtml(input);

        // Confirm the visible banana became banana™
        assertThat(result).contains("banana™");

        // Confirm the script content is unchanged
        assertThat(result).contains("<script>var banana = 123456;</script>");
    }

    @Test
    void rewritesInternalHrefAndSrcAttributes() {
        String input = """
                <html><body>
                <a href="/about">About</a>
                <img src="/images/logo.png" />
                </body></html>
                """;

        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("href=\"/proxy/about\"");
        assertThat(result).contains("src=\"/proxy/images/logo.png\"");
    }

    @Test
    void doesNotRewriteExternalOrProtocolRelativeLinks() {
        String input = """
                <html><body>
                <a href="https://example.com/page">External</a>
                <script src="//cdn.example.com/lib.js"></script>
                </body></html>
                """;

        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("href=\"https://example.com/page\"");
        assertThat(result).contains("src=\"//cdn.example.com/lib.js\"");
    }

    @Test
    void modifiesDeeplyNestedTextNodes() {
        String input = """
                <html><body>
                <div><section><p>banana orange</p></section></div>
                </body></html>
                """;

        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("banana™");
        assertThat(result).contains("orange™");
    }

    @Test
    void doesNotBreakHtmlStructure() {
        String input = """
            <html><body>
            <h1>Header</h1>
            <p>This is cherry content with banana words.</p>
            </body></html>
            """;

        String result = htmlModifierService.modifyHtml(input);

        assertThat(result).contains("<h1>Header™</h1>");
        assertThat(result).contains("<p>This is cherry™ content with banana™ words.</p>");
    }

    @Test
    void handlesEmptyHtml() {
        String result = htmlModifierService.modifyHtml("");
        assertThat(result).isNotNull();
        assertThat(result).contains("<html");
    }
}