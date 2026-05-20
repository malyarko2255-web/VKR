package ru.finuniversity.advance.notification.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateEngine {

    public String render(String template, Map<String, String> vars) {
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
