package com.davigama.assessflow.livesession.application;

/**
 * Neutralizes spreadsheet formula injection in user-controlled CSV text.
 * Numeric fields must not be passed through this method.
 */
public final class CsvFormulaGuard {
    private CsvFormulaGuard() {}

    public static String escape(String value) {
        String text = value == null ? "" : value;
        if (isFormula(text)) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    public static boolean isFormula(String text) {
        if (text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
    }
}
