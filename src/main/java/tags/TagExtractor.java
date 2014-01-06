/**
 * 
 */
package tags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v23Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * @author Abhinav Sharma
 */
public class TagExtractor {

  private final String               ARTISTS_META_FILE_PREFIX = "ARTISTS_META_FILE";
  private final String               SONGS_META_FILE_PREFIX   = "SONGS_META_FILE";

  private final String               GENRES_META_FILE         = "GENRES_META_FILE.txt";

  private final String               dirName;
  private final String               artistFileName;
  private final String               songFileName;

  private ArrayList<HashSet<String>> artists;
  private final List<Song>           songs                    = new ArrayList<Song>();
  private final List<String>         genres                   = new ArrayList<String>();

  public TagExtractor(String dirName) throws NoSuchAlgorithmException {
    this.dirName = dirName;
    this.artistFileName = getFileName(dirName, ARTISTS_META_FILE_PREFIX);
    this.songFileName = getFileName(dirName, SONGS_META_FILE_PREFIX);
  }

  public void run() throws IOException, ClassNotFoundException {

    // read genres from file
    try {
      readAllGenres();
    } catch (URISyntaxException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    // initialize artist according to the number of genres
    artists = new ArrayList<HashSet<String>>(genres.size() + 1);
    for (int i = 0; i < genres.size() + 1; ++i) {
      artists.add(new HashSet<String>());
    }

    // case: we have meta files!
    if (new File(songFileName).exists() && new File(artistFileName).exists()) {
      readMetaFiles();
      return;
    }

    // go through all mp3s in the dir and fill the DSs
    Path fn = FileSystems.getDefault().getPath(dirName);
    Files.walkFileTree(fn, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".mp3")) {
          try {
            extractId3v2Tags(file);
          } catch (UnsupportedTagException e) {
            e.printStackTrace();
          } catch (InvalidDataException e) {
            e.printStackTrace();
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });

    // make meta files for faster retrieval next time
    writeMetaFiles();
  }

  public List<Song> getSongs() {
    return songs;
  }

  public List<String> getGenres() {
    return genres;
  }

  public List<String> getArtists() {
    List<String> ret = new ArrayList<String>();
    for (HashSet<String> hs : artists) {
      ret.addAll(hs);
    }
    return ret;
  }

  public int getArtistIndex(Song song) {
    int pos = getGenreIndex(song) + 1;
    int index = new ArrayList<String>(artists.get(pos)).indexOf(song.getTag().getArtist());
    for (int i = 0; i <= pos; ++i) {
      index += artists.get(i).size();
    }
    return index;
  }

  public int getGenreIndex(Song song) {
    return genres.indexOf(song.getTag().getGenreDescription());
  }

  private String getFileName(String dir, String prefix) throws NoSuchAlgorithmException {
    return DigestUtils.md5Hex(prefix + dir) + ".txt";
  }

  private void extractId3v2Tags(Path filename) throws UnsupportedTagException, InvalidDataException, IOException {
    Mp3File songFile = new Mp3File(filename.toString());
    if (songFile.hasId3v2Tag()) {
      ID3v2 id3v2tag = songFile.getId3v2Tag();
      if (id3v2tag.getGenreDescription() != null && id3v2tag.getArtist() != null) {
        Song song = new Song(id3v2tag, filename.toAbsolutePath().toString());
        // TODO: we're adding it in the end, that means they are not logically
        // related. Not cool!
        if (!genres.contains(song.getTag().getGenreDescription())) {
          genres.add(song.getTag().getGenreDescription());
          artists.add(new HashSet<String>());
        }
        addArtist(song);
        songs.add(song);
      }
    }
  }

  private void addArtist(Song song) {
    int pos = getGenreIndex(song) + 1;
    artists.get(pos).add(song.getTag().getArtist());
  }

  private void readAllGenres() throws IOException, URISyntaxException {
    BufferedReader br;
    if (new File(GENRES_META_FILE).exists()) {
      br = new BufferedReader(new FileReader(new File(GENRES_META_FILE)));
    } else {
      br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + GENRES_META_FILE)));
    }
    String line;
    while ((line = br.readLine()) != null) {
      genres.add(line);
    }
    br.close();
  }

  private void writeMetaFiles() throws IOException {
    // write artists meta file
    FileOutputStream fos = new FileOutputStream(artistFileName);
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(artists);
    oos.close();

    // write songs meta file
    writeSongsMetaFile();

    // write genre meta file
    try {
      writeGenreMetaFile();
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  private void readMetaFiles() throws IOException, ClassNotFoundException {

    // read artists meta file
    FileInputStream fos = new FileInputStream(artistFileName);
    ObjectInputStream oos = new ObjectInputStream(fos);
    artists = (ArrayList<HashSet<String>>) oos.readObject();
    oos.close();

    // read songs meta file
    readSongsMetaFile();
  }

  private void readSongsMetaFile() throws IOException {
    FileReader fileReader = new FileReader(new File(songFileName));
    BufferedReader br = new BufferedReader(fileReader);
    String line = null;

    while ((line = br.readLine()) != null) {
      String array[] = line.split("\\|");
      ID3v23Tag tag = new ID3v23Tag();
      tag.setAlbum(array[0]);
      tag.setAlbumArtist(array[1]);
      tag.setArtist(array[2]);
      tag.setTitle(array[3]);
      tag.setGenre(Integer.parseInt(array[4]));
      songs.add(new Song(tag, array[5]));
    }
    br.close();
  }

  private void writeSongsMetaFile() throws FileNotFoundException, UnsupportedEncodingException {

    PrintWriter writer = new PrintWriter(songFileName);
    for (Song tag : songs) {
      writer.println(tag.getTag().getAlbum() + "|" + tag.getTag().getAlbumArtist() + "|" + tag.getTag().getArtist() + "|"
          + tag.getTag().getTitle() + "|" + tag.getTag().getGenre() + "|" + tag.getFileName());
    }
    writer.close();
  }

  private void writeGenreMetaFile() throws FileNotFoundException, URISyntaxException {
    PrintWriter writer = new PrintWriter(new File(GENRES_META_FILE));
    for (String genre : genres) {
      writer.println(genre);
    }
    writer.close();
  }
}
