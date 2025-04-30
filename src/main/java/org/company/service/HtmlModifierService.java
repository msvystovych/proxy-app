package org.company.service;


import org.company.util.ProxyConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to modify HTML content by adding a trademark symbol to six-letter words
 * and rewriting internal links.
 */
@Service
public class HtmlModifierService {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w{6}\\b");

    public String modifyHtml(String html, String path) {
        Document doc = Jsoup.parse(html);

        modifyTextNodes(doc);
        rewriteInternalLinks(doc);

        return doc.outerHtml();
    }

    private void modifyTextNodes(Document document) {
        for (TextNode textNode : document.textNodes()) {
            String updatedText = addTrademarkToSixLetterWords(textNode.text());
            textNode.text(updatedText);
        }
    }

    private void rewriteInternalLinks(Document document) {
        for (Element element : document.select("[href], [src]")) {
            String attr = element.hasAttr("href") ? "href" : "src";
            String url = element.attr(attr);
            if (isInternalLink(url)) {
                element.attr(attr, "/proxy" + normalizeUrl(url));
            }
        }
    }

    private boolean isInternalLink(String url) {
        return url.startsWith("/") && !url.startsWith("//");
    }

    private String normalizeUrl(String url) {
        return url.startsWith("/") ? url : "/" + url;
    }

    private String addTrademarkToSixLetterWords(String text) {
        Matcher matcher = WORD_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group() + ProxyConstants.TM_MARK);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}