package ph.agap.app.alerts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Annex A alert list, read from assets/annex_a.tsv. Each line is
 * "category TAB alert TAB YAMNet class"; YAMNet class names can contain commas, so only tabs split.
 */
public final class AnnexA {

    public static final String ASSET = "annex_a.tsv";

    private final List<AlertType> alerts;
    private final Map<String, AlertType> byClass;

    private AnnexA(List<AlertType> alerts, Map<String, AlertType> byClass) {
        this.alerts = Collections.unmodifiableList(alerts);
        this.byClass = Collections.unmodifiableMap(byClass);
    }

    public static AnnexA parse(InputStream in) throws IOException {
        Map<String, List<String>> classesByAlert = new LinkedHashMap<>();
        Map<String, AlertCategory> categoryByAlert = new LinkedHashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            String[] cols = line.split("\t", -1);
            if (cols.length != 3) {
                throw new IOException("annex_a.tsv line " + lineNo + ": expected 3 tab-separated columns");
            }
            AlertCategory category = AlertCategory.fromDisplayName(cols[0].trim());
            String alert = cols[1].trim();
            AlertCategory known = categoryByAlert.put(alert, category);
            if (known != null && known != category) {
                throw new IOException("annex_a.tsv line " + lineNo + ": " + alert + " has two categories");
            }
            classesByAlert.computeIfAbsent(alert, k -> new ArrayList<>()).add(cols[2].trim());
        }

        List<AlertType> alerts = new ArrayList<>();
        Map<String, AlertType> byClass = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : classesByAlert.entrySet()) {
            AlertType type = new AlertType(entry.getKey(), categoryByAlert.get(entry.getKey()), entry.getValue());
            alerts.add(type);
            for (String yamnetClass : type.yamnetClasses) {
                if (byClass.put(yamnetClass, type) != null) {
                    throw new IOException("annex_a.tsv: " + yamnetClass + " is mapped to two alerts");
                }
            }
        }
        return new AnnexA(alerts, byClass);
    }

    /** The alerts in file order. */
    public List<AlertType> alerts() {
        return alerts;
    }

    /** The alert a YAMNet class belongs to, or null if the class is not in Annex A. */
    public AlertType alertFor(String yamnetClass) {
        return byClass.get(yamnetClass);
    }

    /** Every mapped YAMNet class name. */
    public java.util.Set<String> yamnetClasses() {
        return byClass.keySet();
    }
}
