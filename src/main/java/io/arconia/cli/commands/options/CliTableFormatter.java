package io.arconia.cli.commands.options;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Help.TextTable;

/**
 * Formats data into aligned tabular text for CLI output using Picocli's {@link TextTable}.
 * <p>
 * Column widths are computed automatically from the data. The last column uses
 * wrapping overflow; all others use span overflow (no truncation).
 */
public final class CliTableFormatter {

    private static final int COLUMN_PADDING = 2;

    private CliTableFormatter() {}

    /**
     * Formats headers and rows into an aligned table string.
     *
     * @param colorScheme the Picocli color scheme (controls ANSI rendering)
     * @param headers     the column header labels
     * @param rows        the data rows; each inner list must have the same size as {@code headers}
     * @return the formatted table as a single string (may contain newlines)
     */
    public static String format(ColorScheme colorScheme, List<String> headers, List<List<String>> rows) {
        return format(colorScheme, headers, rows, 0);
    }

    /**
     * Formats headers and rows into an aligned table string, with each line prefixed by {@code indent} spaces.
     *
     * @param colorScheme the Picocli color scheme (controls ANSI rendering)
     * @param headers     the column header labels
     * @param rows        the data rows; each inner list must have the same size as {@code headers}
     * @param indent      number of leading spaces to prepend to each line
     * @return the formatted table as a single string (may contain newlines)
     */
    public static String format(ColorScheme colorScheme, List<String> headers, List<List<String>> rows, int indent) {
        Assert.notNull(colorScheme, "colorScheme cannot be null");
        Assert.notEmpty(headers, "headers cannot be empty");
        Assert.notNull(rows, "rows cannot be null");

        int[] widths = computeWidths(headers, rows);
        TextTable table = TextTable.forColumnWidths(colorScheme, widths);

        table.addRowValues(headers.toArray(String[]::new));
        for (List<String> row : rows) {
            table.addRowValues(row.toArray(String[]::new));
        }

        String result = table.toString();
        if (indent <= 0) {
            return result;
        }
        String prefix = " ".repeat(indent);
        return result.lines()
                .map(line -> prefix + line)
                .collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator();
    }

    private static int[] computeWidths(List<String> headers, List<List<String>> rows) {
        int n = headers.size();
        int[] widths = new int[n];
        for (int i = 0; i < n; i++) {
            widths[i] = headers.get(i).length() + COLUMN_PADDING;
        }
        for (List<String> row : rows) {
            for (int i = 0; i < Math.min(row.size(), n); i++) {
                String cell = row.get(i);
                if (cell != null) {
                    widths[i] = Math.max(widths[i], cell.length() + COLUMN_PADDING);
                }
            }
        }
        return widths;
    }

}
