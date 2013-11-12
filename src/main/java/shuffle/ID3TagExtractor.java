/**
 * 
 */
package shuffle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.la4j.factory.Factory;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic1DMatrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * @author abhinav.s
 * 
 */
public class ID3TagExtractor {

	public static ID3TagExtractor tagExtractor = new ID3TagExtractor();

	private static SortedSet<String> artists = new TreeSet<String>();
	private static List<ID3v2> songs = new ArrayList<ID3v2>();
	private static SortedSet<String> genres = new TreeSet<String>();

	private final static String ARTISTS_META_FILE_NAME = "ARTISTS_META_FILE.txt";
	private final static String SONGS_META_FILE_NAME = "SONGS_META_FILE.txt";
	private final static String GENRES_META_FILE_NAME = "GENRES_META_FILE.txt";
	
	private Matrix modelCoeff;

	public void extract(String folderName) throws IOException {
		Path fn = FileSystems.getDefault().getPath(folderName);
		Files.walkFileTree(fn, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				if (file.toString().endsWith(".mp3")) {
					try {
						tagExtractor.extractId3v2Tags(file);
					} catch (UnsupportedTagException e) {
						e.printStackTrace();
					} catch (InvalidDataException e) {
						e.printStackTrace();
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		//		makeMetaFiles();
	}

	@SuppressWarnings("unchecked")
	public void extractFromMetaFiles() throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(ARTISTS_META_FILE_NAME);
		ObjectInputStream ois = new ObjectInputStream(fis);
		artists = (SortedSet<String>) ois.readObject();
		ois.close();

		fis = new FileInputStream(SONGS_META_FILE_NAME);
		ois = new ObjectInputStream(fis);
		songs = (List<ID3v2>) ois.readObject();
		ois.close();

		fis = new FileInputStream(GENRES_META_FILE_NAME);
		ois = new ObjectInputStream(fis);
		genres = (SortedSet<String>) ois.readObject();
		ois.close();
	}

	private void extractId3v2Tags(Path filename) throws UnsupportedTagException,
	InvalidDataException, IOException {
		Mp3File song = new Mp3File(filename.toString());
		if (song.hasId3v2Tag()) {
			ID3v2 id3v2tag = song.getId3v2Tag();
			if (id3v2tag.getGenreDescription() != null
					&& id3v2tag.getArtist() != null) {
				artists.add(id3v2tag.getArtist());
				songs.add(id3v2tag);
				genres.add(id3v2tag.getGenreDescription());
			}
		}
	}

	private void makeMetaFiles() throws IOException {
		FileOutputStream fos = new FileOutputStream(ARTISTS_META_FILE_NAME);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(artists);
		oos.close();

		fos = new FileOutputStream(SONGS_META_FILE_NAME);
		oos = new ObjectOutputStream(fos);
		oos.writeObject(songs);
		oos.close();

		fos = new FileOutputStream(GENRES_META_FILE_NAME);
		oos = new ObjectOutputStream(fos);
		oos.writeObject(genres);
		oos.close();
	}
	
	private Matrix solveMatrix(double[][] dimensions, double[][] facts) {
		Matrix dim = new Basic2DMatrix(dimensions);
		Matrix f = new Basic2DMatrix(facts);
		return dim.transpose().multiply(f);
	}
	
	private boolean isPlayable(Matrix song, double tolerance) {
		return (Math.abs(song.get(0, 0) - modelCoeff.get(0, 0)) / modelCoeff.get(0, 0) < tolerance && Math.abs(song.get(1, 0) - modelCoeff.get(1, 0)) / modelCoeff.get(1, 0) < tolerance);
	}
	
	@SuppressWarnings("deprecation")
	public void testModel() {
		double [][] dimensions;
		double [][] facts;
		dimensions = new double[100][2];
		facts = new double[100][1];
		Random random = new Random(new Date().getTime());
		for(int i = 0; i < 100; ++i) {
			dimensions[i][0] = random.nextInt();
			dimensions[i][1] = random.nextInt();
			facts[i][0] = random.nextInt() % 2;
		}
		
		modelCoeff = solveMatrix(dimensions, facts);
		System.out.println("Model: Artist=" + modelCoeff.get(0, 0) + " Genre=" + modelCoeff.get(1, 0));

		runModel();
	}

	public void runModel() {
		for (ID3v2 tag : songs) {
			int artist = Collections.binarySearch(new ArrayList<String>(artists), tag.getArtist());
			int genre = Collections.binarySearch(new ArrayList<String>(genres), tag.getGenreDescription());
			
			double dimensions[][] = new double[1][2];
			double facts[][] = new double[1][1];
			
			dimensions[0][0] = artist;
			dimensions[0][1] = genre;
			facts[0][0] = 1.0;
			
			Matrix currentSong = solveMatrix(dimensions, facts);
			
			double tolerance = 0.01; // percent tolerance
			
			if (isPlayable(currentSong, tolerance))
				System.out.println(tag.getTitle() + " " + tag.getArtist() + " " + tag.getGenreDescription());
		}
	}

	public static void main(String[] args) throws UnsupportedTagException,
	InvalidDataException, IOException {
		tagExtractor.extract("D://My Music//English");	
		tagExtractor.testModel();
	}
}
