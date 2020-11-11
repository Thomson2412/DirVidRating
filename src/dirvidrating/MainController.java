package dirvidrating;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {
    @FXML
    public GridPane container;

    @FXML
    public Button prevBtn;
    @FXML
    public Button nextBtn;

    @FXML
    public Button gotoIndexBtn;
    @FXML
    public TextField gotoIndexTxt;

    @FXML
    public HBox ratingButtons;

    @FXML
    public Label realVidLabel;
    @FXML
    private MediaView mediaViewLeft;
    @FXML
    public Label currVidLabel;
    @FXML
    private MediaView mediaViewRight;

    @FXML
    public Button selectDirBtn;
    @FXML
    private Label dirLabel;

    private List<Path> dataList = null;
    private int indexCount = 0;

    @FXML
    public void initialize() {
        prevBtn.setDisable(true);
        nextBtn.setDisable(true);
        gotoIndexBtn.setDisable(true);
        gotoIndexTxt.setDisable(true);
        setDisableRatingButtons(true);

        gotoIndexTxt.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*") || newValue.length() == 0) {
                gotoIndexTxt.setText(newValue.replaceAll("[^\\d]", ""));
            }
            else {
                int newIndex = Integer.parseInt(newValue);
                gotoIndexBtn.setDisable(newIndex < 0 || newIndex > dataList.size() - 1);
            }
        });

        mediaViewLeft.fitWidthProperty().bind(container.widthProperty().divide(2));
        mediaViewLeft.fitHeightProperty().bind(container.heightProperty().divide(2));

        mediaViewRight.fitWidthProperty().bind(container.widthProperty().divide(2));
        mediaViewRight.fitHeightProperty().bind(container.heightProperty().divide(2));
    }

    @FXML
    private void playPrev() {
        if (indexCount > 0)
            indexCount--;
        setDisabledForIndexCount(indexCount);
        setIndexText(indexCount);
        playVideo(indexCount);
    }

    @FXML
    private void playNext() {
        if (indexCount < dataList.size())
            indexCount++;
        setDisabledForIndexCount(indexCount);
        setIndexText(indexCount);
        playVideo(indexCount);
    }

    @FXML
    private void gotoIndexBtn() {
        int newIndex = Integer.parseInt(gotoIndexTxt.getText());
        if(newIndex != indexCount && newIndex < dataList.size()) {
            indexCount = newIndex;
            setDisabledForIndexCount(indexCount);
            playVideo(newIndex);
        }
    }

    private void playVideo(int index) {
        if (dataList == null || dataList.size() == 0 || index > dataList.size() - 1)
            return;

        mediaViewLeft.setMediaPlayer(null);
        mediaViewRight.setMediaPlayer(null);

        Path videoPath = dataList.get(index);
        setAndCreateMediaPlayer(videoPath, mediaViewRight);
        currVidLabel.setText(videoPath.toFile().getName());

        Path realVideoPath = getReal(videoPath);
        if (realVideoPath != null) {
            setAndCreateMediaPlayer(realVideoPath, mediaViewLeft);
            realVidLabel.setText(realVideoPath.toFile().getName());
        }
    }

    private void setAndCreateMediaPlayer(Path videoPath, MediaView mediaView) {
        Media videoMedia = new Media(videoPath.toUri().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(videoMedia);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaView.setMediaPlayer(mediaPlayer);
    }

    @FXML
    private void selectDir() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dir = directoryChooser.showDialog(new Stage());
        if (dir != null && createDatalistFromDir(dir.toPath())) {
            dirLabel.setText(dir.toString());
            indexCount = 0;
            playVideo(indexCount);
            setIndexText(indexCount);
            setDisabledForIndexCount(indexCount);
            setDisableRatingButtons(false);
            gotoIndexBtn.setDisable(false);
            gotoIndexTxt.setDisable(false);
        }
    }

    private boolean createDatalistFromDir(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            dataList = walk
                    .filter(path -> !path.toString().contains("/Real/"))
                    .filter(path -> path.toFile().isFile())
                    .filter(path -> path.toString().endsWith(".mp4")).collect(Collectors.toList());
            Collections.sort(dataList);
            if(dataList.size() > 0)
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Path getReal(Path path) {
        String filename = path.getFileName().toString();
        String[] splitFilenameExtension = filename.split("\\.");
        String[] splitFilename = splitFilenameExtension[0].split("_");
        if (splitFilename.length >= 5) {
            String realDirString = path.getParent().getParent().toString() + "/Real";
            Path realDirPath = Paths.get(realDirString);

            try (Stream<Path> walk = Files.walk(realDirPath)) {
                List<Path> result = walk
                        .filter(p -> p.toFile().isFile())
                        .filter(p -> p.toString().split("\\.")[0].endsWith(splitFilename[4])).collect(Collectors.toList());

                if (result.size() == 1)
                    return result.get(0);

            } catch (IOException e) {
                realVidLabel.setText("Real not found");
            }
        }

        return null;
    }

    @FXML
    private void ratingBtn(ActionEvent event) {
        String rating = ((Control) event.getSource()).getId();
        Path currVideoPath = dataList.get(indexCount);

        String currParentPathString = currVideoPath.getParent().toString();
        String currFileName = currVideoPath.getFileName().toString();

        String[] splitFilenameExtension = currFileName.split("\\.");
        if (splitFilenameExtension.length == 2) {
            String[] splitFilename = splitFilenameExtension[0].split("_");
            if (splitFilename.length > 5)
                splitFilenameExtension[0] = String.join("_", Arrays.copyOf(splitFilename, splitFilename.length - 1));
            File renameFile = new File(
                    String.format("%s/%s_%s.%s",
                            currParentPathString,
                            splitFilenameExtension[0],
                            rating,
                            splitFilenameExtension[1]));

            if (currVideoPath.toFile().renameTo(renameFile)) {
                currVidLabel.setText(renameFile.getName());
                dataList.set(indexCount, renameFile.toPath());
            }
        }
    }

    private void setDisableRatingButtons(boolean disabled){
        for (Node btn: ratingButtons.getChildren()) {
            btn.setDisable(disabled);
        }
    }

    private void setDisabledForIndexCount(int index){
        prevBtn.setDisable(index == 0);
        nextBtn.setDisable(index == dataList.size() - 1);
    }

    private void setIndexText(int index){
        gotoIndexTxt.setText(Integer.toString(index));
    }
}
