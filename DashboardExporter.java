import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DashboardExporter {
    static class BenchmarkRow {
        final String algorithm;
        final long dataSize;
        final String mode;
        final int threads;
        final long timeMs;
        final boolean sorted;

        BenchmarkRow(String algorithm, long dataSize, String mode, int threads, long timeMs, boolean sorted) {
            this.algorithm = algorithm;
            this.dataSize = dataSize;
            this.mode = mode;
            this.threads = threads;
            this.timeMs = timeMs;
            this.sorted = sorted;
        }
    }

    static class ConfigKey {
        final String algorithm;
        final long dataSize;
        final String mode;
        final int threads;

        ConfigKey(String algorithm, long dataSize, String mode, int threads) {
            this.algorithm = algorithm;
            this.dataSize = dataSize;
            this.mode = mode;
            this.threads = threads;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConfigKey)) return false;
            ConfigKey c = (ConfigKey) o;
            return dataSize == c.dataSize
                    && threads == c.threads
                    && algorithm.equals(c.algorithm)
                    && mode.equals(c.mode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(algorithm, dataSize, mode, threads);
        }
    }

    static class Stats {
        double avg;
        long min;
        long max;
        int samples;
        boolean sorted;

        Stats(double avg, long min, long max, int samples, boolean sorted) {
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.samples = samples;
            this.sorted = sorted;
        }
    }

    public static List<BenchmarkRow> readCsv(String path) throws IOException {
        List<BenchmarkRow> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(path));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] p = line.split(",");
            if (p.length < 7) continue;
            rows.add(new BenchmarkRow(
                    p[0].trim(),
                    Long.parseLong(p[1].trim()),
                    p[2].trim(),
                    Integer.parseInt(p[3].trim()),
                    Long.parseLong(p[5].trim()),
                    Boolean.parseBoolean(p[6].trim())));
        }
        return rows;
    }

    public static Map<ConfigKey, Stats> aggregate(List<BenchmarkRow> rows) {
        Map<ConfigKey, List<BenchmarkRow>> grouped = new LinkedHashMap<>();
        for (BenchmarkRow row : rows) {
            grouped.computeIfAbsent(
                    new ConfigKey(row.algorithm, row.dataSize, row.mode, row.threads),
                    key -> new ArrayList<>()).add(row);
        }

        Map<ConfigKey, Stats> result = new LinkedHashMap<>();
        for (Map.Entry<ConfigKey, List<BenchmarkRow>> entry : grouped.entrySet()) {
            List<BenchmarkRow> values = entry.getValue();
            double avg = values.stream().mapToLong(row -> row.timeMs).average().orElse(0);
            long min = values.stream().mapToLong(row -> row.timeMs).min().orElse(0);
            long max = values.stream().mapToLong(row -> row.timeMs).max().orElse(0);
            boolean sorted = values.stream().allMatch(row -> row.sorted);
            result.put(entry.getKey(), new Stats(avg, min, max, values.size(), sorted));
        }
        return result;
    }

    public static String buildJsonData(List<BenchmarkRow> rows, Map<ConfigKey, Stats> stats) {
        Set<String> algorithms = new LinkedHashSet<>();
        Set<Long> sizes = new LinkedHashSet<>();
        for (BenchmarkRow row : rows) {
            algorithms.add(row.algorithm);
            sizes.add(row.dataSize);
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"algorithms\":").append(toJsonStringArray(algorithms)).append(",");
        sb.append("\"sizes\":").append(toJsonNumberArray(sizes)).append(",");
        sb.append("\"stats\":[");

        int index = 0;
        for (Map.Entry<ConfigKey, Stats> entry : stats.entrySet()) {
            ConfigKey key = entry.getKey();
            Stats value = entry.getValue();
            if (index++ > 0) sb.append(",");
            sb.append("{");
            sb.append("\"algorithm\":\"").append(escape(key.algorithm)).append("\",");
            sb.append("\"dataSize\":").append(key.dataSize).append(",");
            sb.append("\"mode\":\"").append(escape(key.mode)).append("\",");
            sb.append("\"threads\":").append(key.threads).append(",");
            sb.append("\"avg\":").append(String.format(Locale.US, "%.1f", value.avg)).append(",");
            sb.append("\"min\":").append(value.min).append(",");
            sb.append("\"max\":").append(value.max).append(",");
            sb.append("\"samples\":").append(value.samples).append(",");
            sb.append("\"sorted\":").append(value.sorted);
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private static String toJsonStringArray(Set<String> values) {
        StringBuilder sb = new StringBuilder("[");
        int index = 0;
        for (String value : values) {
            if (index++ > 0) sb.append(",");
            sb.append("\"").append(escape(value)).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String toJsonNumberArray(Set<Long> values) {
        StringBuilder sb = new StringBuilder("[");
        int index = 0;
        for (Long value : values) {
            if (index++ > 0) sb.append(",");
            sb.append(value);
        }
        return sb.append("]").toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String buildHtml(String json) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Benchmark de Ordenacao</title>
                <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js"></script>
                <style>
                *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
                body{font-family:system-ui,sans-serif;background:#f7f7f5;color:#1f2428}
                header{background:#fff;border-bottom:1px solid #ddd;padding:1rem 1.5rem;display:flex;gap:.75rem;align-items:center;flex-wrap:wrap}
                h1{font-size:1.1rem;font-weight:650}
                header span{font-size:12px;color:#667;background:#eef2f6;padding:3px 9px;border-radius:999px}
                main{max-width:1180px;margin:0 auto;padding:1.25rem}
                .controls{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:1rem}
                select{height:34px;border:1px solid #cfd6dd;border-radius:6px;background:#fff;padding:0 10px;color:#1f2428}
                .kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;margin-bottom:1rem}
                .kpi{background:#fff;border:1px solid #e0e4e8;border-radius:8px;padding:.9rem}
                .kpi-label{font-size:12px;color:#68737d;margin-bottom:3px}
                .kpi-value{font-size:20px;font-weight:650}
                .grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}
                .panel{background:#fff;border:1px solid #e0e4e8;border-radius:8px;padding:1rem}
                .panel.full{grid-column:1/-1}
                .panel-title{font-size:13px;color:#4f5b66;margin-bottom:.75rem}
                .chart-frame{position:relative;height:290px}
                @media(max-width:760px){.grid{grid-template-columns:1fr}.chart-frame{height:250px}}
                </style>
                </head>
                <body>
                <header><h1>Benchmark de Ordenacao</h1><span>serial e paralelo</span></header>
                <main>
                <div class="controls">
                  <select id="algorithm"></select>
                  <select id="size"></select>
                </div>
                <div class="kpi-grid" id="kpis"></div>
                <div class="grid">
                  <section class="panel"><p class="panel-title">Tempo medio por configuracao (ms)</p><div class="chart-frame"><canvas id="timeChart"></canvas></div></section>
                  <section class="panel"><p class="panel-title">Speedup vs versao sequencial do mesmo algoritmo</p><div class="chart-frame"><canvas id="speedChart"></canvas></div></section>
                  <section class="panel full"><p class="panel-title">Minimo, media e maximo das amostras (ms)</p><div class="chart-frame"><canvas id="rangeChart"></canvas></div></section>
                </div>
                </main>
                <script>
                const DATA = __JSON__;
                const colors = ['#4b5563','#2563eb','#059669','#dc6b2f','#7c3aed','#c026d3'];
                let charts = {};
                const algorithmSelect = document.getElementById('algorithm');
                const sizeSelect = document.getElementById('size');

                function labelSize(size){return Intl.NumberFormat('pt-BR').format(size)+' elementos';}
                function destroy(){Object.values(charts).forEach(chart => chart.destroy()); charts = {};}
                function rows(){return DATA.stats.filter(r => r.algorithm === algorithmSelect.value && String(r.dataSize) === sizeSelect.value);}
                function seqRow(){return rows().find(r => r.mode === 'Sequential' && r.threads === 1);}
                function rowLabel(r){return r.mode === 'Sequential' ? 'Serial 1T' : 'Paralelo '+r.threads+'T';}

                function fillControls(){
                  algorithmSelect.innerHTML = DATA.algorithms.map(a => `<option value="${a}">${a}</option>`).join('');
                  sizeSelect.innerHTML = DATA.sizes.map(s => `<option value="${s}">${labelSize(s)}</option>`).join('');
                }

                function renderKpis(currentRows){
                  const fastest = [...currentRows].sort((a,b) => a.avg - b.avg)[0];
                  const sequential = seqRow();
                  const speedup = sequential && fastest ? sequential.avg / fastest.avg : 0;
                  const allSorted = currentRows.every(r => r.sorted);
                  document.getElementById('kpis').innerHTML = [
                    ['Configuracoes', currentRows.length],
                    ['Mais rapido', fastest ? rowLabel(fastest) : '-'],
                    ['Melhor tempo medio', fastest ? fastest.avg.toFixed(1)+' ms' : '-'],
                    ['Speedup max.', speedup ? speedup.toFixed(2)+'x' : '-'],
                    ['Ordenacao validada', allSorted ? 'Sim' : 'Nao']
                  ].map(k => `<div class="kpi"><p class="kpi-label">${k[0]}</p><p class="kpi-value">${k[1]}</p></div>`).join('');
                }

                function render(){
                  destroy();
                  const currentRows = rows();
                  const labels = currentRows.map(rowLabel);
                  const sequential = seqRow();
                  renderKpis(currentRows);

                  charts.time = new Chart('timeChart',{type:'bar',data:{labels,datasets:[{
                    data:currentRows.map(r => r.avg), backgroundColor:colors, borderRadius:4, borderSkipped:false
                  }]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},scales:{y:{beginAtZero:true}}}});

                  const parallelRows = currentRows.filter(r => r.mode === 'Parallel');
                  charts.speed = new Chart('speedChart',{type:'line',data:{
                    labels:parallelRows.map(r => r.threads+'T'),
                    datasets:[{label:'Real',data:parallelRows.map(r => sequential ? +(sequential.avg/r.avg).toFixed(2) : 0),
                      borderColor:'#2563eb',backgroundColor:'rgba(37,99,235,.12)',fill:true,tension:.25,pointRadius:4},
                      {label:'Ideal',data:parallelRows.map(r => r.threads),borderColor:'rgba(31,36,40,.35)',borderDash:[6,4],pointRadius:0}]
                  },options:{responsive:true,maintainAspectRatio:false,scales:{y:{beginAtZero:true}}}});

                  charts.range = new Chart('rangeChart',{type:'bar',data:{labels,datasets:[
                    {label:'Min',data:currentRows.map(r => r.min),backgroundColor:'rgba(37,99,235,.22)',stack:'range'},
                    {label:'Media - min',data:currentRows.map(r => +(r.avg-r.min).toFixed(1)),backgroundColor:'rgba(5,150,105,.55)',stack:'range'},
                    {label:'Max - media',data:currentRows.map(r => +(r.max-r.avg).toFixed(1)),backgroundColor:'rgba(220,107,47,.45)',stack:'range'}
                  ]},options:{responsive:true,maintainAspectRatio:false,scales:{x:{stacked:true},y:{stacked:true,beginAtZero:true}}}});
                }

                fillControls();
                algorithmSelect.addEventListener('change', render);
                sizeSelect.addEventListener('change', render);
                render();
                </script>
                </body>
                </html>
                """.replace("__JSON__", json);
    }

    public static void export(String csvPath, String outputHtml, boolean openBrowser) throws IOException {
        System.out.println("Lendo: " + csvPath);
        List<BenchmarkRow> rows = readCsv(csvPath);
        System.out.println("  -> " + rows.size() + " linhas lidas.");
        Map<ConfigKey, Stats> stats = aggregate(rows);
        Files.writeString(Path.of(outputHtml), buildHtml(buildJsonData(rows, stats)));
        System.out.println("Dashboard gerado: " + outputHtml);

        Path htmlPath = Path.of(outputHtml).toAbsolutePath();
        System.out.println("Abra: file://" + htmlPath);
        if (openBrowser) {
            openInBrowser(htmlPath);
        }
    }

    private static void openInBrowser(Path htmlPath) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(htmlPath.toUri());
                System.out.println("Dashboard aberto no navegador.");
            }
        } catch (IOException e) {
            System.out.println("Nao foi possivel abrir automaticamente: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        String csv = args.length > 0 ? args[0] : "benchmark_results.csv";
        String html = args.length > 1 ? args[1] : "dashboard.html";
        boolean openBrowser = args.length < 3 || !"--no-open".equals(args[2]);
        export(csv, html, openBrowser);
    }
}
