/*
 * TilingCollection.java Copyright (C) 2019. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tegula.tilingcollection;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import jloda.fx.control.AnotherMultipleSelectionModel;
import jloda.fx.util.AService;
import jloda.fx.window.NotificationManager;
import jloda.util.Basic;
import jloda.util.FileLineIterator;
import jloda.util.ProgressListener;
import tegula.core.dsymbols.DSymbol;
import tegula.db.DatabaseAccess;
import tegula.util.IFileBased;
import tegula.util.ImageEncoding;

import java.io.Closeable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * a collection of tilings
 * Daniel Huson, 4.2019
 */
public class TilingCollection implements IFileBased {
    private final StringProperty fileName = new SimpleStringProperty("Untitled");

    private final ObservableList<DSymbol> dSymbols = FXCollections.observableArrayList();
    private final Map<DSymbol, Image> dSymbolImageMap = new HashMap<>();
    private final IntegerProperty size = new SimpleIntegerProperty(0);
    private final AnotherMultipleSelectionModel<DSymbol> selectionModel;

    private final IntegerProperty blockSize = new SimpleIntegerProperty(60);

    private AService<Boolean> loadingService;

    /**
     * setup a new tiling collection
     *
     * @param fileName
     */
    public TilingCollection(String fileName) {
        setFileName(fileName);
        size.bind((Bindings.size(dSymbols)));
        this.selectionModel = new AnotherMultipleSelectionModel<>();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        dSymbols.addListener((InvalidationListener) (c) -> {
            selectionModel.setItems(dSymbols);
        });
    }

    /**
     * load tilings from file
     *
     * @param statusPane
     * @param runAfterwards if non null, will be run in FX thread after loading
     */
    public void load(Pane statusPane, Runnable runAfterwards) {
        loadingService = new AService<>(statusPane);

        loadingService.setCallable(() -> {
            final ProgressListener progress = loadingService.getProgressListener();
                    progress.setTasks("Loading", getFileName());
                    progress.setMaximum(Basic.guessUncompressedSizeOfFile(getFileName()));

            final Iterator<String> iterator;
            if (getFileName().startsWith("select:"))
                iterator = DatabaseAccess.getInstance().getDSymbols(getFileName().replaceAll("^select:", "")).iterator();
            else
                iterator = new FileLineIterator(getFileName(), true);

            try {
                        final ArrayList<DSymbol> cache = new ArrayList<>(getBlockSize());
                if (iterator.hasNext()) {
                    String line = iterator.next().trim();

                    while (true) {
                        if (line.startsWith("<") && line.endsWith(">")) {
                            final DSymbol dSymbol = new DSymbol();
                            dSymbol.read(new StringReader(line));
                            cache.add(dSymbol);
                            if (!iterator.hasNext())
                                break;
                            line = iterator.next().trim();
                            if (line.startsWith("[") && line.endsWith("]")) {
                                final Image image = ImageEncoding.decodeImage(line.substring(1, line.length() - 1));
                                dSymbolImageMap.put(dSymbol, image);
                                if (!iterator.hasNext())
                                    break;
                                line = iterator.next().trim();
                            }

                            if (cache.size() == getBlockSize()) {
                                final DSymbol[] array = cache.toArray(new DSymbol[0]);
                                cache.clear();
                                Platform.runLater(() -> dSymbols.addAll(array));
                            }
                            progress.setProgress(progress.getProgress() + getBlockSize()); // wild guess
                        } else {
                            if (!iterator.hasNext())
                                break;
                            line = iterator.next();
                        }
                    }
                    if (cache.size() > 0)
                        Platform.runLater(() -> dSymbols.addAll(cache));
                }
            } finally {
                if (iterator instanceof Closeable)
                    ((Closeable) iterator).close();
                    }
            return true;
                }
        );
        loadingService.setOnCancelled((e) -> NotificationManager.showInformation(String.format("CANCELED, loaded %,d tilings", getSize())));
        loadingService.setOnSucceeded((e) -> {
            NotificationManager.showInformation(String.format("Loaded %,d tilings", getSize()));
            if (runAfterwards != null)
                runAfterwards.run();
        });
        loadingService.setOnFailed((e) -> NotificationManager.showError("Load failed: " + loadingService.getException().getMessage()));
        loadingService.start();
    }


    public ObservableList<DSymbol> getDSymbols() {
        return dSymbols;
    }

    public AnotherMultipleSelectionModel<DSymbol> getSelectionModel() {
        return selectionModel;
    }

    public int getSize() {
        return size.get();
    }

    public ReadOnlyIntegerProperty sizeProperty() {
        return size;
    }

    public DSymbol findSymbol(String text) {
        final String numbers = Basic.getFirstWord(text);
        for (DSymbol dSymbol : dSymbols) {
            if (numbers.equals(String.format("%d.%d", dSymbol.getNr1(), dSymbol.getNr2())))
                return dSymbol;
        }
        return null;
    }

    public int getBlockSize() {
        return blockSize.get();
    }

    public IntegerProperty blockSizeProperty() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize.set(blockSize);
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public String getTitle() {
        return Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(fileName.get()), "").replaceAll("^select:", "");
    }

    public Image getPreviewImage(DSymbol ds) {
        return dSymbolImageMap.get(ds);
    }

    public void cancelLoading() {
        if (loadingService != null)
            loadingService.cancel();

    }

}
