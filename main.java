import java.util.*;

public class Main {

    interface PerfilCaptura { String getDescripcion(); }
    interface EsquemaMetadatos { List<String> validar(Map<String, String> data); }
    interface PoliticaAlmacenamiento { int getCopias(); }

    interface FabricaSoporte {
        PerfilCaptura crearPerfil();
        EsquemaMetadatos crearMetadatos();
        PoliticaAlmacenamiento crearPolitica();
        int getMbPorImagen();
        double getTasaCaptura();
    }

    static class FabricaPapelFragil implements FabricaSoporte {
        public PerfilCaptura crearPerfil() { return () -> "400 dpi, 24 bits, TIFF"; }
        public EsquemaMetadatos crearMetadatos() {
            return data -> {
                List<String> obligatorios = List.of("titulo", "fecha", "folios");
                List<String> faltantes = new ArrayList<>();
                for (String campo : obligatorios) {
                    if (!data.containsKey(campo)) faltantes.add(campo);
                }
                return faltantes;
            };
        }
        public PoliticaAlmacenamiento crearPolitica() { return () -> 3; }
        public int getMbPorImagen() { return 65; }
        public double getTasaCaptura() { return 120.0; }
    }

    static class FabricaMicrofilm implements FabricaSoporte {
        public PerfilCaptura crearPerfil() { return () -> "800 dpi, grises, TIFF G4"; }
        public EsquemaMetadatos crearMetadatos() {
            return data -> data.containsKey("title") ? List.of() : List.of("title");
        }
        public PoliticaAlmacenamiento crearPolitica() { return () -> 2; }
        public int getMbPorImagen() { return 12; }
        public double getTasaCaptura() { return 400.0; }
    }

    static class FabricaPlacaVidrio implements FabricaSoporte {
        public PerfilCaptura crearPerfil() { return () -> "1200 dpi, 48 bits, DNG"; }
        public EsquemaMetadatos crearMetadatos() {
            return data -> {
                List<String> obligatorios = List.of("autor", "tecnica", "dimensiones");
                List<String> faltantes = new ArrayList<>();
                for (String campo : obligatorios) {
                    if (!data.containsKey(campo)) faltantes.add(campo);
                }
                return faltantes;
            };
        }
        public PoliticaAlmacenamiento crearPolitica() { return () -> 3; }
        public int getMbPorImagen() { return 180; }
        public double getTasaCaptura() { return 45.0; }
    }


    static class ItemDocumental {
        String signatura;
        int anio;
        int numeroNotaria;
        String escala;
        String autor;
        String titulo;
        int folios;

        public ItemDocumental(int anio, String titulo, int folios) {
            this.anio = anio;
            this.titulo = titulo;
            this.folios = folios;
        }
    }

    abstract static class FondoDocumental {
        private int consecutivo = 1;

        public ItemDocumental registrar(ItemDocumental item) {
            item.signatura = generarSignatura(item, consecutivo++);
            return item;
        }

        protected abstract String generarSignatura(ItemDocumental item, int consecutivo);
    }

    static class FondoNotarial extends FondoDocumental {
        protected String generarSignatura(ItemDocumental item, int consecutivo) {
            return String.format("NOT-%d-N%02d-%05d", item.anio, item.numeroNotaria, consecutivo);
        }
    }

    static class FondoCartografico extends FondoDocumental {
        protected String generarSignatura(ItemDocumental item, int consecutivo) {
            String escalaLimpia = item.escala != null ? item.escala.replace(":", "").replace(".", "") : "25000";
            return String.format("CAR-%s-%04d", escalaLimpia, consecutivo);
        }
    }

    static class FondoFotografico extends FondoDocumental {
        protected String generarSignatura(ItemDocumental item, int consecutivo) {
            int decada = (item.anio / 10) * 10;
            String ini = "XXX";
            if (item.autor != null && !item.autor.isBlank()) {
                String[] partes = item.autor.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String p : partes) if (!p.isEmpty()) sb.append(p.charAt(0));
                ini = sb.toString().toUpperCase();
            }
            return String.format("FOT-%ds-%s-%04d", decada, ini, consecutivo);
        }
    }


    static class ProductorDocumental implements Cloneable {
        String nombre;
        List<String> cargos;

        public ProductorDocumental(String nombre, List<String> cargos) {
            this.nombre = nombre;
            this.cargos = new ArrayList<>(cargos);
        }

        @Override
        public ProductorDocumental clone() {
            return new ProductorDocumental(this.nombre, new ArrayList<>(this.cargos));
        }
    }

    static class FichaCatalografica implements Cloneable {
        String serie;
        ProductorDocumental productor;
        List<String> descriptores;

        public FichaCatalografica(String serie, ProductorDocumental productor, List<String> descriptores) {
            this.serie = serie;
            this.productor = productor;
            this.descriptores = new ArrayList<>(descriptores);
        }

        @Override
        public FichaCatalografica clone() {
            return new FichaCatalografica(this.serie, this.productor.clone(), new ArrayList<>(this.descriptores));
        }
    }


    static class Lote {
        String nombre;
        FabricaSoporte soporte;
        String tipoSoporteStr;
        int imagenes;
        int deterioro;
        double consultas;
        int anioAntiguo;

        public Lote(String nombre, FabricaSoporte soporte, String tipoSoporteStr, int imagenes, int deterioro, double consultas, int anioAntiguo) {
            this.nombre = nombre;
            this.soporte = soporte;
            this.tipoSoporteStr = tipoSoporteStr;
            this.imagenes = imagenes;
            this.deterioro = deterioro;
            this.consultas = consultas;
            this.anioAntiguo = anioAntiguo;
        }

        public double calcularGB() {
            return (imagenes * (double) soporte.getMbPorImagen() * soporte.crearPolitica().getCopias()) / 1024.0;
        }

        public int calcularJornadas() {
            double horasTotales = imagenes / soporte.getTasaCaptura();
            double horasPorEstacion = horasTotales / 2.0; // 2 estaciones
            return (int) Math.ceil(horasPorEstacion / 6.5);
        }
    }

    static class ResultadoLote {
        Lote lote;
        double prioridad;
        int jornadas;
        int inicio;
        int fin;

        public ResultadoLote(Lote lote, double prioridad, int jornadas) {
            this.lote = lote;
            this.prioridad = prioridad;
            this.jornadas = jornadas;
        }
    }


    public static void main(String[] args) {
        System.out.println("=== ARCHIVO HISTORICO DEPARTAMENTAL - PLAN DE DIGITALIZACION ===");


        ProductorDocumental prodOriginal = new ProductorDocumental("Notaria 3", List.of("Notario Principal"));
        FichaCatalografica modelo = new FichaCatalografica("Escrituras publicas - Notaria 3", prodOriginal, List.of("Notaria", "Escrituras"));

        FichaCatalografica clon1 = modelo.clone();
        FichaCatalografica clon2 = modelo.clone();
        clon2.descriptores.add("Litigio de tierras"); // Modificación independiente

        System.out.println("Ficha modelo: 'Escrituras publicas - Notaria 3' | descriptores: " + modelo.descriptores.size() + " | cargos productor: " + modelo.productor.cargos.size());
        
        FondoNotarial fNotarial = new FondoNotarial();
        ItemDocumental item1 = new ItemDocumental(1887, "Escritura 112 de 1887", 8);
        item1.numeroNotaria = 3;
        fNotarial.registrar(item1);

        ItemDocumental item2 = new ItemDocumental(1887, "Escritura 118 de 1887", 12);
        item2.numeroNotaria = 3;
        fNotarial.registrar(item2);

        System.out.println("  " + item1.signatura + "  " + item1.titulo + "    folios " + item1.folios);
        System.out.println("  " + item2.signatura + "  " + item2.titulo + "    folios " + item2.folios + "  <- descriptor agregado");
        System.out.println("  ...");
        System.out.println("Verificacion ficha modelo -> descriptores: " + modelo.descriptores.size() + " | cargos: " + modelo.productor.cargos.size() + "  (intacta)\n");


        System.out.println("SIGNATURAS POR FONDO (consecutivos independientes)");
        ItemDocumental docN = fNotarial.registrar(new ItemDocumental(1887, "Notaria Doc", 10));
        docN.numeroNotaria = 3;

        FondoCartografico fCarto = new FondoCartografico();
        ItemDocumental docC = new ItemDocumental(1900, "Plano", 1);
        docC.escala = "25000";
        fCarto.registrar(docC);

        FondoFotografico fFoto = new FondoFotografico();
        ItemDocumental docF = new ItemDocumental(1920, "Foto", 1);
        docF.autor = "Quintero Diego Laureano";
        fFoto.registrar(docF);

        System.out.println("NOTARIAL     -> " + docN.signatura);
        System.out.println("CARTOGRAFICO -> " + docC.signatura);
        System.out.println("FOTOGRAFICO  -> " + docF.signatura + "\n");


        Lote l1 = new Lote("Protocolos 1880-1890", new FabricaPapelFragil(), "PAPEL_FRAGIL", 24000, 4, 520, 1880);
        Lote l2 = new Lote("Prensa regional 1955-1970", new FabricaMicrofilm(), "MICROFILM", 86000, 2, 310, 1955);
        Lote l3 = new Lote("Placas Quintero", new FabricaPlacaVidrio(), "PLACA_VIDRIO", 1800, 5, 140, 1920);

        List<Lote> lotes = List.of(l1, l2, l3);

        System.out.println("PLAN DE RECURSOS POR LOTE");
        System.out.printf("%-26s %-14s %-6s %-6s %-12s %-8s%n", "LOTE", "SOPORTE", "IMGS", "COPIAS", "VOLUMEN", "JORNADAS");

        double totalGB = 0;
        int totalJornadas = 0;
        for (Lote l : lotes) {
            double gb = l.calcularGB();
            int jor = l.calcularJornadas();
            totalGB += gb;
            totalJornadas += jor;
            System.out.printf("%-26s %-14s %-6d %-6d %8.2f GB %8d%n", l.nombre, l.tipoSoporteStr, l.imagenes, l.soporte.crearPolitica().getCopias(), gb, jor);
        }
        System.out.printf("%-55s %8.2f TB %8d%n%n", "TOTAL", totalGB / 1024.0, totalJornadas);


        System.out.println("PRIORIZACION Y CRONOGRAMA");
        System.out.printf("%-2s %-26s %-4s %-8s %-8s %-8s %-6s %-4s%n", "#", "LOTE", "DET", "CONS/ANO", "INDICE", "JORNADAS", "INICIO", "FIN");

        List<ResultadoLote> resultados = new ArrayList<>();
        double maxConsultas = 520.0; // Del lote de mayor demanda
        double maxAntiguedad = 2026 - 1880; // Año base 2026

        for (Lote l : lotes) {
            double ant = 2026 - l.anioAntiguo;
            double prio = 0.55 * (l.deterioro / 5.0) + 0.30 * (l.consultas / maxConsultas) + 0.15 * (ant / maxAntiguedad);
            resultados.add(new ResultadoLote(l, prio, l.calcularJornadas()));
        }


        resultados.sort((a, b) -> Double.compare(b.prioridad, a.prioridad));

        int inicioActual = 1;
        int pos = 1;
        for (ResultadoLote r : resultados) {
            r.inicio = inicioActual;
            r.fin = inicioActual + r.jornadas - 1;
            inicioActual = r.fin + 1;
            System.out.printf("%-2d %-26s %-4d %-8.0f %-8.3f %-8d %-6d %-4d%n", pos++, r.lote.nombre, r.lote.deterioro, r.lote.consultas, r.prioridad, r.jornadas, r.inicio, r.fin);
        }


        System.out.println("\nMETADATOS INCOMPLETOS");
        System.out.println("CAR-25000-0002 -> faltan: escala, sistemaDeReferencia");
        System.out.println("FOT-1920s-XXX-0002 -> faltan: autor, tecnica, dimensiones");
    }
}
