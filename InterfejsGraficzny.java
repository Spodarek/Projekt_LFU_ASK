import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class InterfejsGraficzny extends JFrame {
    private JTextField poleSekwencja;
    private JTextField poleRozmiarCache;
    private JButton przyciskSymuluj;
    private JButton przyciskLosowa;
    private JPanel panelWizualizacji;
    private JTextArea poleLog;
    private SymulatorLFU symulator;
    private List<Integer> sekwencja;
    private int aktualnyKrok = 0;
    private Timer timer;

    public InterfejsGraficzny() {
        setTitle("Symulator LFU - Zastępowanie Stron");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Górny panel z inputem ===
        JPanel panelInput = new JPanel();
        poleSekwencja = new JTextField(30);
        poleRozmiarCache = new JTextField(5);
        przyciskSymuluj = new JButton("Symuluj");
        przyciskLosowa = new JButton("Losowa sekwencja");

        panelInput.add(new JLabel("Sekwencja stron (np. 1,2,3,1,4):"));
        panelInput.add(poleSekwencja);
        panelInput.add(new JLabel("Rozmiar cache:"));
        panelInput.add(poleRozmiarCache);
        panelInput.add(przyciskSymuluj);
        panelInput.add(przyciskLosowa);

        add(panelInput, BorderLayout.NORTH);

        // === Wizualizacja ramek cache ===
        panelWizualizacji = new PanelWizualizacji();

        // === Log tekstowy (wyższy i ładny) ===
        poleLog = new JTextArea(18, 80);
        poleLog.setEditable(false);
        poleLog.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        poleLog.setBackground(new Color(240, 240, 240));
        poleLog.setForeground(Color.BLACK);
        poleLog.setLineWrap(true);
        poleLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(poleLog);

        // === PRZESUWANY PASEK MIĘDZY WIZUALIZACJĄ A LOGIEM ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                panelWizualizacji, scrollLog);
        splitPane.setResizeWeight(0.7);           // 70% na górę (wizualizacja) na start
        splitPane.setOneTouchExpandable(true);    // strzałki do szybkiego rozwijania
        splitPane.setDividerSize(10);             // grubszy pasek – łatwiej złapać myszką

        add(splitPane, BorderLayout.CENTER);

        // Akcje przycisków
        przyciskSymuluj.addActionListener(e -> startSymulacja());
        przyciskLosowa.addActionListener(e -> generujLosowaSekwencje());

        // Timer – animacja co 1 sekunda
        timer = new Timer(1000, e -> animujKrok());
    }

    private void generujLosowaSekwencje() {
        Random rand = new Random();
        int dlugosc = 10 + rand.nextInt(11);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dlugosc; i++) {
            sb.append(rand.nextInt(10) + 1).append(",");
        }
        poleSekwencja.setText(sb.substring(0, sb.length() - 1));
    }

    private void startSymulacja() {
        try {
            int rozmiarCache = Integer.parseInt(poleRozmiarCache.getText().trim());
            if (rozmiarCache <= 0) {
                JOptionPane.showMessageDialog(this, "Rozmiar cache musi być > 0!");
                return;
            }

            symulator = new SymulatorLFU(rozmiarCache);

            String tekst = poleSekwencja.getText().trim();
            if (tekst.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Wpisz sekwencję stron!");
                return;
            }

            String[] czesci = tekst.split(",");
            sekwencja = new ArrayList<>();
            for (String czesc : czesci) {
                sekwencja.add(Integer.parseInt(czesc.trim()));
            }

            poleLog.setText("=== Rozpoczęcie symulacji LFU ===\n");
            poleLog.append("Rozmiar cache: " + rozmiarCache + "\n");
            poleLog.append("Sekwencja: " + tekst + "\n\n");

            aktualnyKrok = 0;
            timer.start();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Błąd: Wpisz tylko liczby oddzielone przecinkami!");
        }
    }

    private void animujKrok() {
        if (aktualnyKrok >= sekwencja.size()) {
            timer.stop();
            poleLog.append("\n=== Symulacja zakończona ===\n");
            poleLog.append("Łączna liczba błędów strony: " + symulator.pobierzLiczbeBledow() + "\n");
            return;
        }

        int strona = sekwencja.get(aktualnyKrok);

        // Sprawdzamy HIT/MISS
        boolean jestHit = false;
        Collection<Strona> aktualneStrony = symulator.pobierzStronyWCache();
        for (Strona s : aktualneStrony) {
            if (s.numerStrony == strona) {
                jestHit = true;
                break;
            }
        }

        poleLog.append(String.format("Krok %2d: Odwołanie do strony %2d → %s\n",
                aktualnyKrok + 1, strona, jestHit ? "HIT" : "MISS"));

        symulator.odwolanieDoStrony(strona);

        poleLog.append("   Aktualny stan cache: " + symulator.pokazStanCacheDoStringa() + "\n\n");

        panelWizualizacji.repaint();
        aktualnyKrok++;
    }

    // Panel rysujący ramki cache
    class PanelWizualizacji extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(new Font("Arial", Font.BOLD, 16));

            Collection<Strona> strony = (symulator != null) ? symulator.pobierzStronyWCache() : null;

            if (strony == null || strony.isEmpty()) {
                g.setColor(Color.RED);
                g.drawString("Cache jest pusty – zacznij symulację", 50, 100);
                return;
            }

            int rozmiarBoxa = 120;
            int margines = 30;
            int x = 50;
            int y = 50;

            for (Strona s : strony) {
                g.setColor(new Color(173, 216, 230));
                g.fillRoundRect(x, y, rozmiarBoxa, rozmiarBoxa + 30, 20, 20);

                g.setColor(Color.BLACK);
                g.drawRoundRect(x, y, rozmiarBoxa, rozmiarBoxa + 30, 20, 20);

                g.drawString("Strona: " + s.numerStrony, x + 15, y + 40);
                g.drawString("Użycia: " + s.czestotliwosc, x + 15, y + 70);

                x += rozmiarBoxa + margines;

                if (x + rozmiarBoxa > getWidth()) {
                    x = 50;
                    y += rozmiarBoxa + 60;
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new InterfejsGraficzny().setVisible(true);
        });
    }
}
