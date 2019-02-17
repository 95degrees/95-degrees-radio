package me.voidinvoid.discordmusic.karaoke.lyrics;

import me.voidinvoid.discordmusic.utils.FormattingUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SongLyrics {

    private List<LyricLine> lyrics = new ArrayList<>();

    public SongLyrics(String rawXml) throws ParserConfigurationException, SAXException, IOException {

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(rawXml)));
        doc.getDocumentElement().normalize();

        NodeList transRoot = doc.getElementsByTagName("transcript").item(0).getChildNodes();

        for (int i = 0; i <= transRoot.getLength(); i++) {
            Node node = transRoot.item(i);

            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {

                Element lyric = (Element) node;
                lyrics.add(new LyricLine(lyric.getFirstChild() == null ? "" : lyric.getFirstChild().getTextContent(), Double.valueOf(lyric.getAttribute("start")), Double.valueOf(lyric.getAttribute("dur"))));
            }
        }

        System.out.println("Found lyrics for song!");
    }

    public String getMessage(String songName, double elapsed) {
        LyricLine active = getActiveLyric(elapsed);

        StringBuilder builder = new StringBuilder("[").append(FormattingUtils.escapeMarkup(songName)).append(" Song Lyrics]\n\n");

        for (LyricLine l : lyrics) {
            builder.append(l.equals(active) ? "➡" : "◼").append(FormattingUtils.escapeMarkup(l.getText())).append("\n");
        }

        System.out.println(builder.toString());

        return builder.toString();
    }

    public double getNextLyricEntryTime(double elapsed) {
        for (LyricLine l : lyrics) {
            if (l.getEntryTime() > elapsed) return l.getEntryTime();
        }

        return -1;
    }

    public List<LyricLine> getLyrics() {
        return lyrics;
    }

    public LyricLine getActiveLyric(double elapsed) {

        for (LyricLine l : lyrics) {
            if ((l.getEntryTime() <= elapsed) && (l.getEntryTime() + l.getLength() >= elapsed)) return l;
        }

        return null;
    }
}
