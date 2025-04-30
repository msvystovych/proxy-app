package org.company.service;


import org.company.util.ProxyConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HtmlModifierService {

    private static final Pattern SIX_LETTER_WORD_PATTERN = Pattern.compile("\\b\\w{6}\\b");

    /**
     * Modifies the HTML content by:
     * 1. Adding a trademark symbol (™) to each six-letter word.
     * 2. Rewriting internal links to go through the proxy.
     *
     * @param html The original HTML content.
     * @return The modified HTML content.
     */
    public String modifyHtml(String html) {
        Document doc = Jsoup.parse(html);

        // Modify visible text content
        modifyTextNodes(doc.body());

        // Rewrite internal navigation links
        rewriteInternalLinks(doc);

        return doc.outerHtml();
    }

    /**
     * Recursively modifies all text nodes in the document body
     */
    private void modifyTextNodes(Node node) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                String updatedText = addTrademarkToSixLetterWords(textNode.text());
                textNode.text(updatedText);
            } else {
                modifyTextNodes(child);
            }
        }
    }

    /**
     * Rewrites internal links (href, src) to go through the proxy
     */
    private void rewriteInternalLinks(Document document) {
        Elements elements = document.select("[href], [src]");
        for (Element element : elements) {
            String attr = element.hasAttr("href") ? "href" : "src";
            String url = element.attr(attr);

            if (isInternalLink(url)) {
                element.attr(attr, "/proxy" + normalizeUrl(url));
            }
        }
    }

    /**
     * Detects internal links (e.g., "/spring3/"), skips "//" and external domains
     */
    private boolean isInternalLink(String url) {
        return url.startsWith("/") && !url.startsWith("//");
    }

    /**
     * Ensures the link is prefixed with a single slash (avoids //double)
     */
    private String normalizeUrl(String url) {
        return url.startsWith("/") ? url : "/" + url;
    }

    /**
     * Adds ™ to each six-letter word in the given text
     */
    private String addTrademarkToSixLetterWords(String text) {
        Matcher matcher = SIX_LETTER_WORD_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group() + ProxyConstants.TM_MARK);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}