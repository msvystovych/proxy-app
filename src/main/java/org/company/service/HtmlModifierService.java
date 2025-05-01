package org.company.service;


import org.company.util.ProxyConstants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HtmlModifierService {

    private static final Pattern SIX_LETTER_WORD_PATTERN = Pattern.compile("\\b\\w{6}\\b");

    public String modifyHtml(String html) {
        Document doc = Jsoup.parse(html);

        // Modify all visible text content
        modifyTextNodesIteratively(doc.body());

        // Rewrite all internal links
        rewriteInternalLinks(doc);

        return doc.outerHtml();
    }

    /**
     * Iteratively modifies all text nodes in the DOM using a stack
     */
    private void modifyTextNodesIteratively(Node root) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Node current = stack.pop();

            if (current instanceof TextNode textNode) {
                textNode.text(addTrademarkToSixLetterWords(textNode.text()));
            }

            List<Node> children = current.childNodes();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    /**
     * Rewrites internal links in href and src attributes to go through the proxy
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

    private boolean isInternalLink(String url) {
        return url.startsWith("/") && !url.startsWith("//");
    }

    private String normalizeUrl(String url) {
        return url.startsWith("/") ? url : "/" + url;
    }

    /**
     * Adds ™ to every six-letter word in the input text
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