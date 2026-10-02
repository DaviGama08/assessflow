package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.livesession.application.CsvFormulaGuard;
import org.junit.jupiter.api.Test;

class CsvFormulaGuardTest {
    @Test
    void quotesCommasNewlinesAndNeutralizesFormulas() {
        assertThat(CsvFormulaGuard.escape("plain")).isEqualTo("\"plain\"");
        assertThat(CsvFormulaGuard.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvFormulaGuard.escape("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(CsvFormulaGuard.escape("line\nbreak")).isEqualTo("\"line\nbreak\"");
        assertThat(CsvFormulaGuard.escape("=SUM(A1)")).isEqualTo("\"'=SUM(A1)\"");
        assertThat(CsvFormulaGuard.escape("+cmd")).isEqualTo("\"'+cmd\"");
        assertThat(CsvFormulaGuard.escape("-1+A1")).isEqualTo("\"'-1+A1\"");
        assertThat(CsvFormulaGuard.escape("@import")).isEqualTo("\"'@import\"");
        assertThat(CsvFormulaGuard.escape("José")).isEqualTo("\"José\"");
        assertThat(CsvFormulaGuard.escape(null)).isEqualTo("\"\"");
        assertThat(CsvFormulaGuard.isFormula("12")).isFalse();
    }
}
