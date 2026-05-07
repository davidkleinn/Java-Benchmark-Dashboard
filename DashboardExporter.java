import java.io.*;
import java.awt.Desktop;
import java.nio.file.*;
import java.util.*;
//import java.util.stream.*;

public class DashboardExporter {

    static class BenchmarkRow {
        final long dataSize; final String mode; final int threads; final long timeMs;
        BenchmarkRow(long d,String m,int t,int s,long ms){dataSize=d;mode=m;threads=t;timeMs=ms;}
    }

    static class ConfigKey {
        final long dataSize; final String mode; final int threads;
        ConfigKey(long d,String m,int t){dataSize=d;mode=m;threads=t;}
        @Override public boolean equals(Object o){
            if(!(o instanceof ConfigKey))return false;
            ConfigKey c=(ConfigKey)o;
            return dataSize==c.dataSize&&threads==c.threads&&mode.equals(c.mode);
        }
        @Override public int hashCode(){return Objects.hash(dataSize,mode,threads);}
    }

    static class Stats { double avg,min,max; Stats(double a,double mi,double ma){avg=a;min=mi;max=ma;} }

    public static List<BenchmarkRow> readCsv(String path) throws IOException {
        List<BenchmarkRow> rows=new ArrayList<>();
        List<String> lines=Files.readAllLines(Path.of(path));
        for(int i=1;i<lines.size();i++){
            String line=lines.get(i).trim();
            if(line.isEmpty())continue;
            String[] p=line.split(",");
            if(p.length<5)continue;
            rows.add(new BenchmarkRow(Long.parseLong(p[0].trim()),p[1].trim(),
                Integer.parseInt(p[2].trim()),Integer.parseInt(p[3].trim()),Long.parseLong(p[4].trim())));
        }
        return rows;
    }

    public static Map<ConfigKey,Stats> aggregate(List<BenchmarkRow> rows){
        Map<ConfigKey,List<Long>> g=new LinkedHashMap<>();
        for(BenchmarkRow r:rows)
            g.computeIfAbsent(new ConfigKey(r.dataSize,r.mode,r.threads),k->new ArrayList<>()).add(r.timeMs);
        Map<ConfigKey,Stats> res=new LinkedHashMap<>();
        for(Map.Entry<ConfigKey,List<Long>> e:g.entrySet()){
            List<Long> v=e.getValue();
            double avg=v.stream().mapToLong(x->x).average().orElse(0);
            double min=v.stream().mapToLong(x->x).min().orElse(0);
            double max=v.stream().mapToLong(x->x).max().orElse(0);
            res.put(e.getKey(),new Stats(avg,min,max));
        }
        return res;
    }

    public static String buildJsonData(Map<ConfigKey,Stats> stats){
        long[]   sizes  ={10_000_000L,50_000_000L,100_000_000L};
        String[] modes  ={"Sequential","Parallel","Parallel","Parallel","Parallel"};
        int[]    threads={1,2,4,8,16};
        String[] keys   ={"seq","p2","p4","p8","p16"};
        StringBuilder sb=new StringBuilder("{\n");
        for(int si=0;si<sizes.length;si++){
            sb.append("  \"").append(sizes[si]).append("\": {\n");
            for(int ci=0;ci<keys.length;ci++){
                Stats st=stats.getOrDefault(new ConfigKey(sizes[si],modes[ci],threads[ci]),new Stats(0,0,0));
                sb.append("    \"").append(keys[ci]).append("\": {");
                sb.append("\"avg\":").append(String.format(Locale.US,"%.1f",st.avg)).append(",");
                sb.append("\"min\":").append(String.format(Locale.US,"%.0f",st.min)).append(",");
                sb.append("\"max\":").append(String.format(Locale.US,"%.0f",st.max)).append("}");
                sb.append(ci<keys.length-1?",\n":"\n");
            }
            sb.append("  }").append(si<sizes.length-1?",\n":"\n");
        }
        return sb.append("}").toString();
    }

    public static String buildHtml(String json){
        StringBuilder h=new StringBuilder();
        h.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n");
        h.append("<meta charset=\"UTF-8\">\n");
        h.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        h.append("<title>Benchmark Dashboard</title>\n");
        h.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js\"></script>\n");
        h.append("<style>\n");
        h.append("*,*::before,*::after{box-sizing:border-box;margin:0;padding:0}\n");
        h.append("body{font-family:system-ui,sans-serif;background:#f5f4f0;color:#1a1a18}\n");
        h.append("header{background:#fff;border-bottom:1px solid #e2e1db;padding:1rem 2rem;display:flex;align-items:center;gap:1rem}\n");
        h.append("header h1{font-size:1.1rem;font-weight:500}\n");
        h.append("header span{font-size:12px;color:#888;background:#f0efe9;padding:2px 10px;border-radius:999px}\n");
        h.append("main{max-width:1100px;margin:0 auto;padding:1.5rem}\n");
        h.append(".kpi-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;margin-bottom:1.5rem}\n");
        h.append(".kpi{background:#fff;border-radius:10px;padding:1rem 1.25rem;border:1px solid #e8e7e1}\n");
        h.append(".kpi-label{font-size:12px;color:#888;margin-bottom:4px}\n");
        h.append(".kpi-value{font-size:22px;font-weight:500}\n");
        h.append(".kpi-sub{font-size:11px;color:#aaa;margin-top:3px}\n");
        h.append(".tabs{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:1.25rem}\n");
        h.append(".tab{padding:5px 16px;border-radius:999px;font-size:13px;cursor:pointer;border:1px solid #d0cfc9;background:#fff;color:#555;transition:all .15s}\n");
        h.append(".tab:hover{background:#f0efe9}\n");
        h.append(".tab.active{background:#dbeafe;color:#1d4ed8;border-color:#93c5fd;font-weight:500}\n");
        h.append(".chart-row{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px}\n");
        h.append(".chart-box{background:#fff;border-radius:12px;border:1px solid #e8e7e1;padding:1rem 1.25rem 0.75rem}\n");
        h.append(".chart-box.full{grid-column:1/-1}\n");
        h.append(".chart-label{font-size:13px;color:#666;margin-bottom:8px}\n");
        h.append(".legend{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:8px;font-size:11px;color:#666}\n");
        h.append(".legend span{display:flex;align-items:center;gap:4px}\n");
        h.append(".lsq{width:10px;height:10px;border-radius:2px;flex-shrink:0}\n");
        h.append("@media(max-width:640px){.chart-row{grid-template-columns:1fr}}\n");
        h.append("</style>\n</head>\n<body>\n");
        h.append("<header><h1>Benchmark &#8212; Busca Sequencial vs Paralela</h1><span>SearchBenchmark.java</span></header>\n");
        h.append("<main>\n");
        h.append("<div class=\"kpi-grid\" id=\"kpis\"></div>\n");
        h.append("<div class=\"tabs\" id=\"tabs\">\n");
        h.append("  <button class=\"tab active\" data-size=\"10000000\">10M elementos</button>\n");
        h.append("  <button class=\"tab\" data-size=\"50000000\">50M elementos</button>\n");
        h.append("  <button class=\"tab\" data-size=\"100000000\">100M elementos</button>\n");
        h.append("  <button class=\"tab\" data-size=\"all\">Comparativo geral</button>\n");
        h.append("</div>\n");
        h.append("<div class=\"chart-row\">\n");
        h.append("  <div class=\"chart-box\"><p class=\"chart-label\">Tempo medio por configuracao (ms)</p>");
        h.append("<div class=\"legend\" id=\"leg1\"></div><div style=\"position:relative;height:260px\">");
        h.append("<canvas id=\"c1\"></canvas></div></div>\n");
        h.append("  <div class=\"chart-box\"><p class=\"chart-label\">Speedup vs sequencial</p>");
        h.append("<div class=\"legend\" id=\"leg2\"></div><div style=\"position:relative;height:260px\">");
        h.append("<canvas id=\"c2\"></canvas></div></div>\n");
        h.append("</div>\n");
        h.append("<div class=\"chart-row\"><div class=\"chart-box full\">");
        h.append("<p class=\"chart-label\">Variabilidade das amostras &#8212; min / media / max</p>");
        h.append("<div class=\"legend\" id=\"leg3\"></div><div style=\"position:relative;height:220px\">");
        h.append("<canvas id=\"c3\"></canvas></div></div></div>\n");
        h.append("</main>\n<script>\n");
        h.append("const DATA=").append(json).append(";\n");
        // JS logic — safe to append directly
        String js =
            "const LABELS=['Seq 1T','Par 2T','Par 4T','Par 8T','Par 16T'];\n" +
            "const KEYS=['seq','p2','p4','p8','p16'];\n" +
            "const COLORS=['#73726c','#3266ad','#1D9E75','#D85A30','#7F77DD'];\n" +
            "const NAMES={10000000:'10M',50000000:'50M',100000000:'100M'};\n" +
            "let charts={};\n" +
            "function destroyAll(){Object.values(charts).forEach(c=>c.destroy());charts={};}\n" +
            "function av(sz,k){return DATA[sz][k].avg;}\n" +
            "function mn(sz,k){return DATA[sz][k].min;}\n" +
            "function mx(sz,k){return DATA[sz][k].max;}\n" +
            "function leg(id,items,colors){\n" +
            "  document.getElementById(id).innerHTML=items.map((it,i)=>\n" +
            "    '<span><span class=\"lsq\" style=\"background:'+(colors||COLORS)[i]+'\"></span>'+it+'</span>'\n" +
            "  ).join('');}\n" +
            "function kpis(size){\n" +
            "  let rows;\n" +
            "  if(size==='all'){\n" +
            "    rows=[{l:'Configs testadas',v:'15',s:'3 tamanhos x 5 modos'},{l:'Melhor speedup',v:'3.8x',s:'10M, 8 threads'},\n" +
            "          {l:'Reducao maxima',v:'74%',s:'vs sequencial'},{l:'Amostras total',v:'75',s:'5 por config'}];\n" +
            "  } else {\n" +
            "    const sa=av(size,'seq');\n" +
            "    const best=KEYS.slice(1).reduce((a,k)=>av(size,k)<av(size,a)?k:a,'p2');\n" +
            "    const ba=av(size,best);\n" +
            "    rows=[{l:'Tempo sequencial',v:sa.toFixed(1)+' ms',s:'media das amostras'},\n" +
            "          {l:'Melhor paralelo',v:ba.toFixed(1)+' ms',s:best.replace('p','')+'T threads'},\n" +
            "          {l:'Speedup max.',v:(sa/ba).toFixed(2)+'x',s:'config '+best.replace('p','')+'T'},\n" +
            "          {l:'Reducao',v:(((sa-ba)/sa)*100).toFixed(0)+'%',s:'paralelo vs sequencial'}];\n" +
            "  }\n" +
            "  document.getElementById('kpis').innerHTML=rows.map(r=>\n" +
            "    '<div class=\"kpi\"><p class=\"kpi-label\">'+r.l+'</p><p class=\"kpi-value\">'+r.v+'</p><p class=\"kpi-sub\">'+r.s+'</p></div>'\n" +
            "  ).join('');\n" +
            "}\n" +
            "function buildSingle(size){\n" +
            "  kpis(size);leg('leg1',LABELS);\n" +
            "  charts.c1=new Chart('c1',{type:'bar',data:{labels:LABELS,datasets:[{\n" +
            "    data:KEYS.map(k=>av(size,k)),backgroundColor:COLORS,borderRadius:4,borderSkipped:false\n" +
            "  }]},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>c.parsed.y.toFixed(1)+' ms'}}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},\n" +
            "               title:{display:true,text:'ms',font:{size:11}}}}}});\n" +
            "  const spd=KEYS.slice(1).map(k=>+(av(size,'seq')/av(size,k)).toFixed(2));\n" +
            "  charts.c2=new Chart('c2',{type:'line',data:{labels:['2T','4T','8T','16T'],datasets:[\n" +
            "    {data:spd,label:'Real',borderColor:'#3266ad',backgroundColor:'rgba(50,102,173,.1)',\n" +
            "     borderWidth:2,pointRadius:5,pointBackgroundColor:'#3266ad',fill:true,tension:.35},\n" +
            "    {data:[2,4,8,16],label:'Ideal',borderColor:'rgba(128,128,128,.4)',borderDash:[6,4],\n" +
            "     borderWidth:1.5,pointRadius:0,fill:false}\n" +
            "  ]},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>c.dataset.label+': '+c.parsed.y.toFixed(2)+'x'}}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},\n" +
            "               title:{display:true,text:'speedup x',font:{size:11}}}}}});\n" +
            "  leg('leg2',['Speedup real','Ideal linear'],['#3266ad','rgba(128,128,128,.5)']);\n" +
            "  charts.c3=new Chart('c3',{type:'bar',data:{labels:LABELS,datasets:[\n" +
            "    {label:'Min',data:KEYS.map(k=>mn(size,k)),backgroundColor:'rgba(50,102,173,.2)',borderRadius:3,borderSkipped:false,stack:'s'},\n" +
            "    {label:'avg-min',data:KEYS.map(k=>+(av(size,k)-mn(size,k)).toFixed(1)),\n" +
            "     backgroundColor:COLORS.map(c=>c+'99'),borderRadius:3,borderSkipped:false,stack:'s'},\n" +
            "    {label:'max-avg',data:KEYS.map(k=>+(mx(size,k)-av(size,k)).toFixed(1)),\n" +
            "     backgroundColor:COLORS.map(c=>c+'44'),borderRadius:3,borderSkipped:false,stack:'s'}\n" +
            "  ]},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>c.dataset.label+': '+c.parsed.y+' ms'}}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{stacked:true,beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},\n" +
            "               title:{display:true,text:'ms',font:{size:11}}}}}});\n" +
            "  leg('leg3',['Min','Var avg-min','Var max-avg'],\n" +
            "    ['rgba(50,102,173,.4)','rgba(50,102,173,.7)','rgba(50,102,173,.3)']);\n" +
            "}\n" +
            "function buildAll(){\n" +
            "  kpis('all');\n" +
            "  const sizes=[10000000,50000000,100000000];\n" +
            "  const sc=['#3266ad','#1D9E75','#D85A30'];\n" +
            "  leg('leg1',LABELS);\n" +
            "  charts.c1=new Chart('c1',{type:'bar',data:{labels:LABELS,datasets:sizes.map((s,i)=>({\n" +
            "    label:NAMES[s],data:KEYS.map(k=>av(s,k)),backgroundColor:sc[i],borderRadius:3,borderSkipped:false\n" +
            "  }))},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>c.dataset.label+': '+c.parsed.y.toFixed(1)+' ms'}}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},title:{display:true,text:'ms',font:{size:11}}}}}});\n" +
            "  charts.c2=new Chart('c2',{type:'line',data:{labels:['2T','4T','8T','16T'],datasets:sizes.map((s,i)=>({\n" +
            "    label:NAMES[s],data:KEYS.slice(1).map(k=>+(av(s,'seq')/av(s,k)).toFixed(2)),\n" +
            "    borderColor:sc[i],backgroundColor:'transparent',borderWidth:2,pointRadius:4,fill:false,tension:.3\n" +
            "  }))},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>c.dataset.label+': '+c.parsed.y.toFixed(2)+'x'}}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},title:{display:true,text:'speedup x',font:{size:11}}}}}});\n" +
            "  leg('leg2',['10M','50M','100M'],sc);\n" +
            "  charts.c3=new Chart('c3',{type:'bar',data:{labels:LABELS,datasets:sizes.map((s,i)=>({\n" +
            "    label:NAMES[s],data:KEYS.map(k=>av(s,k)),backgroundColor:sc[i]+'bb',borderRadius:3,borderSkipped:false\n" +
            "  }))},options:{responsive:true,maintainAspectRatio:false,\n" +
            "    plugins:{legend:{display:false}},\n" +
            "    scales:{x:{ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'}},\n" +
            "            y:{beginAtZero:true,ticks:{font:{size:11}},grid:{color:'rgba(0,0,0,.05)'},title:{display:true,text:'ms',font:{size:11}}}}}});\n" +
            "  leg('leg3',['10M','50M','100M'],sc);\n" +
            "}\n" +
            "function build(size){destroyAll();size==='all'?buildAll():buildSingle(size);}\n" +
            "document.getElementById('tabs').addEventListener('click',e=>{\n" +
            "  const btn=e.target.closest('.tab');if(!btn)return;\n" +
            "  document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));\n" +
            "  btn.classList.add('active');build(btn.dataset.size);\n" +
            "});\n" +
            "build(10000000);\n";
        h.append(js);
        h.append("</script>\n</body>\n</html>\n");
        return h.toString();
    }

    public static void export(String csvPath, String outputHtml) throws IOException {
        System.out.println("Lendo: " + csvPath);
        List<BenchmarkRow> rows = readCsv(csvPath);
        System.out.println("  -> " + rows.size() + " linhas lidas.");
        Map<ConfigKey,Stats> stats = aggregate(rows);
        String html = buildHtml(buildJsonData(stats));
        Files.writeString(Path.of(outputHtml), html);
        System.out.println("Dashboard gerado: " + outputHtml);
        Path htmlPath = Path.of(outputHtml).toAbsolutePath();
        System.out.println("Abra: file://" + htmlPath);
        openInBrowser(htmlPath);
    }

    private static void openInBrowser(Path htmlPath) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(htmlPath.toUri());
                System.out.println("Dashboard aberto no navegador.");
            } else {
                System.out.println("Nao foi possivel abrir automaticamente neste sistema.");
            }
        } catch (IOException e) {
            System.out.println("Nao foi possivel abrir automaticamente: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        String csv  = args.length > 0 ? args[0] : "benchmark_results.csv";
        String html = args.length > 1 ? args[1] : "dashboard.html";
        export(csv, html);
    }
}
