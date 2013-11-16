/**
 * 
 */
package shuffle;

import java.lang.Math;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v23Tag;
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

	double[][] dimensions = new double[10][2];
	double[][] facts = new double[10][1];

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
		makeMetaFiles();
	}

	@SuppressWarnings("unchecked")
	public void extractFromMetaFiles() throws IOException,
	ClassNotFoundException {
		FileInputStream fis = new FileInputStream(ARTISTS_META_FILE_NAME);
		ObjectInputStream ois = new ObjectInputStream(fis);
		artists = (SortedSet<String>) ois.readObject();
		ois.close();

		readMetaId3();

		fis = new FileInputStream(GENRES_META_FILE_NAME);
		ois = new ObjectInputStream(fis);
		genres = (SortedSet<String>) ois.readObject();
		ois.close();
	}

	private void extractId3v2Tags(Path filename)
			throws UnsupportedTagException, InvalidDataException, IOException {
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

		writeMetaId3();

		fos = new FileOutputStream(GENRES_META_FILE_NAME);
		oos = new ObjectOutputStream(fos);
		oos.writeObject(genres);
		oos.close();
	}

	private void readMetaId3() throws IOException {
		FileReader fileReader = new FileReader(new File(SONGS_META_FILE_NAME));

		BufferedReader br = new BufferedReader(fileReader);

		String line = null;
		// if no more lines the readLine() returns null
		while ((line = br.readLine()) != null) {
			String array[] = line.split("\\|");
			ID3v23Tag tag = new ID3v23Tag();
			tag.setAlbum(array[0]);
			tag.setAlbumArtist(array[1]);
			tag.setArtist(array[2]);
			tag.setTitle(array[3]);
			tag.setGenre(Integer.parseInt(array[4]));
			songs.add(tag);
		}
		br.close();
	}

	private void writeMetaId3() throws FileNotFoundException,
	UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(SONGS_META_FILE_NAME, "UTF-8");
		for (ID3v2 tag : songs) {
			writer.println(tag.getAlbum() + "|" + tag.getAlbumArtist() + "|"
					+ tag.getArtist() + "|" + tag.getTitle() + "|"
					+ tag.getGenre());
		}
		writer.close();
	}

	private Matrix solveMatrix(double[][] dimensions, double[][] facts) {
		Matrix dim = new Basic2DMatrix(dimensions);
		Matrix f = new Basic2DMatrix(facts);
		return dim.transpose().multiply(f);
	}

	private boolean evaluateSong(Matrix songs, double tolerance) {
		return (Math.abs(songs.get(0, 0) - modelCoeff.get(0, 0))
				/ modelCoeff.get(0, 0) < tolerance && Math.abs(songs.get(1, 0)
						- modelCoeff.get(1, 0))
						/ modelCoeff.get(1, 0) < tolerance);
	}

	public void makeModel() {
		modelCoeff = solveMatrix(dimensions, facts);
		System.out.println("Model: Artist=" + modelCoeff.get(0, 0) + " Genre="
				+ modelCoeff.get(1, 0));
	}

	private void consumeTrainingData(int songIndex, int response, int pos) {
		dimensions[pos][0] = Collections.binarySearch(new ArrayList<String> (artists), songs.get(songIndex).getArtist());
		dimensions[pos][1] = Collections.binarySearch(new ArrayList<String> (genres), songs.get(songIndex).getGenreDescription());
		facts[pos][0] = response;
	}

	private boolean isPlayable(int songIndex, double percentTolerance) {

		int artistIndex = Collections.binarySearch(new ArrayList<String> (artists), songs.get(songIndex).getArtist());
		int genreIndex = Collections.binarySearch(new ArrayList<String> (genres), songs.get(songIndex).getGenreDescription());

		double dimensions[][] = new double[1][2];
		double facts[][] = new double[1][1];

		dimensions[0][0] = artistIndex;
		dimensions[0][1] = genreIndex;
		facts[0][0] = 1.0;

		Matrix currentSong = solveMatrix(dimensions, facts);

		return evaluateSong(currentSong, percentTolerance);
	}

	public void test() {
		Random random = new Random(new Date().getTime());
		for (int i = 0; i < 100; ++i) {
			dimensions[i][0] = random.nextInt();
			dimensions[i][1] = random.nextInt();
			facts[i][0] = random.nextInt() % 2;
		}

		makeModel();
		testModel();
	}

	public void testModel() {
		for (int i = 0; i < songs.size(); ++i) {
			ID3v2 tag = songs.get(i);

			if(tag.getArtist() == null || tag.getGenreDescription() == null)
				continue;

			if (isPlayable(i, 0.01))
				System.out.println(tag.getTitle() + " " + tag.getArtist() + " "
						+ tag.getGenreDescription());
		}
	}

	public static void main(String[] args) throws UnsupportedTagException,
	InvalidDataException, IOException {
		if (new File(SONGS_META_FILE_NAME).exists()) {
			try {
				tagExtractor.extractFromMetaFiles();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			tagExtractor.extract("D://My Music//English");
		}

		startDumbPrompt();
		tagExtractor.makeModel();
		startPostLearnPrompt();
	}

	private static void startDumbPrompt() {
		int count = 9;
		Scanner inputScanner = new Scanner(System.in);
		while(count >= 0) {
			int rand = Math.abs(new Random(new Date().getTime()).nextInt()) % songs.size();
			if(songs.get(rand).getArtist() == null || songs.get(rand).getGenreDescription() == null)
				continue;
			System.out.println(songs.get(rand).getArtist() + "-" + songs.get(rand).getGenreDescription());
			int response = inputScanner.nextInt();
			tagExtractor.consumeTrainingData(rand, response, count);
			--count;
		}
		//inputScanner.close();
	}


	private static void startPostLearnPrompt() {
		int count = 0;
		int index = 0;

		Scanner inputScanner = new Scanner(System.in);
		while(count < songs.size()) {

			if(index == 10) {
				index = 0;
				tagExtractor.makeModel();
			}

			if(songs.get(count).getArtist() == null || songs.get(count).getGenreDescription() == null) {
				++count;
				continue;
			}

			if(tagExtractor.isPlayable(count, 1))
				System.out.println(songs.get(count).getArtist() + "-" + songs.get(count).getGenreDescription());
			else {
				++count;
				continue;
			}

			int response = inputScanner.nextInt();
			tagExtractor.consumeTrainingData(count, response, index++);
			++count;
		}
		inputScanner.close();
	}
}

