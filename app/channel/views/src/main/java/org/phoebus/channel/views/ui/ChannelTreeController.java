package org.phoebus.channel.views.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelUtil;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

/**
 * Controller for the Tree view of Channels based on a set of selected properties
 * 
 * @author Kunal Shroff
 *
 */
public class ChannelTreeController extends ChannelFinderController {

    @FXML
    TextField query;
    @FXML
    Button search;
    @FXML
    Button configure;
    @FXML
    CheckBox connect;
    @FXML
    TreeTableView<ChannelTreeByPropertyNode> treeTableView;

    @FXML
    TreeTableColumn<ChannelTreeByPropertyNode, String> node;
    @FXML
    TreeTableColumn<ChannelTreeByPropertyNode, String> value;

    private List<String> orderedProperties = Collections.emptyList();
    private Collection<Channel> channels = Collections.emptyList();
    private ChannelTreeByPropertyModel model;

    @FXML
    public void initialize() {
        dispose();

        node.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getDisplayName()));
        value.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getDisplayValue()));

        treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                final List<Channel> selectedChannels = new ArrayList<Channel>();
                treeTableView.getSelectionModel().getSelectedItems().stream().forEach(item -> {
                    selectedChannels.addAll(item.getValue().getNodeChannels());
                });
                SelectionService.getInstance().setSelection(treeTableView, selectedChannels);
            }
        });
    }

    @FXML
    public void dispose() {
        if(model != null)
        {
            model.dispose();
        }
    }

    public void setQuery(String string) {
        query.setText(string);
        search();
    }

    public void setOrderedProperties(List<String> orderedProperties) {
        this.orderedProperties = orderedProperties;
        reconstructTree();
    }

    @Override
    public void setChannels(Collection<Channel> channels) {
        this.channels = channels;
        reconstructTree();
    }

    /**
     * Dispose the existing model, recreate a new one and 
     */
    @FXML
    private void reconstructTree() {
        dispose();
        model = new ChannelTreeByPropertyModel(query.getText(), channels, orderedProperties, true, connect.isSelected());
        ChannelTreeItem root = new ChannelTreeItem(model.getRoot());
        treeTableView.setRoot(root);
        refreshPvValues();
    }

    /**
     * Reorder the properties used to create the table
     */
    @FXML
    public void configure() {
        if (model != null) {
            List<String> allProperties = ChannelUtil.getPropertyNames(model.getRoot().getNodeChannels()).stream().sorted().collect(Collectors.toList());
            ListMultiOrderedPickerDialog dialog = new ListMultiOrderedPickerDialog(allProperties, orderedProperties);
            Optional<List<String>> result = dialog.showAndWait();
            result.ifPresent(r -> {
                setOrderedProperties(r);
            });
        }
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Refreshes the values of the leaf pv's and append their live values to the tree view of the
     * channels.
     */
    public void refreshPvValues() {
        scheduler.scheduleAtFixedRate(treeTableView::refresh, 0, 400, TimeUnit.MILLISECONDS);
    }

    /**
     * Search channelfinder for the channels satisfying the query set
     */
    @FXML
    public void search() {
        super.search(query.getText());
    }

    @FXML
    public void createContextMenu() {

        final ContextMenu contextMenu = new ContextMenu();

        List<ContextMenuEntry> contextEntries = ContextMenuService.getInstance().listSupportedContextMenuEntries();
        contextEntries.forEach(entry -> {
            MenuItem item = new MenuItem(entry.getName(), new ImageView(entry.getIcon()));
            item.setOnAction(e -> {
                try {
                    SelectionService.getInstance().getSelection();
                    ObservableList<TreeItem<ChannelTreeByPropertyNode>> old = treeTableView.getSelectionModel()
                            .getSelectedItems();

                    List<Object> supportedTypes = SelectionService.getInstance().getSelection().getSelections().stream()
                            .map(s -> {
                                return AdapterService.adapt(s, (Class) entry.getSupportedTypes().get(0)).get();
                            }).collect(Collectors.toList());
                    // set the selection
                    SelectionService.getInstance().setSelection(treeTableView, supportedTypes);
                    entry.callWithSelection(SelectionService.getInstance().getSelection());
                    // reset the selection
                    SelectionService.getInstance().setSelection(treeTableView, old);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
            contextMenu.getItems().add(item);
        });

        treeTableView.setContextMenu(contextMenu);
    }

    /**
     * An implementation for the {@link TreeItem} which dynamically creates the children when required.
     * @author Kunal Shroff
     */
    private final class ChannelTreeItem extends TreeItem<ChannelTreeByPropertyNode> {

        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private boolean isLeaf;

        public ChannelTreeItem(ChannelTreeByPropertyNode node) {
            super(node);
        }

        @Override
        public ObservableList<TreeItem<ChannelTreeByPropertyNode>> getChildren() {
            if (isFirstTimeChildren) {
                isFirstTimeChildren = false;
                super.getChildren().setAll(buildChildren(this));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            if (isFirstTimeLeaf) {
                isFirstTimeLeaf = false;
                if (getValue().getParentNode() == null) {
                    isLeaf = false;
                } else {
                    isLeaf = getValue().getPropertyName() == null;
                }
            }
            return isLeaf;
        }

        private ObservableList<TreeItem<ChannelTreeByPropertyNode>> buildChildren(
                TreeItem<ChannelTreeByPropertyNode> treeItem) {
            ChannelTreeByPropertyNode item = treeItem.getValue();
            ObservableList<TreeItem<ChannelTreeByPropertyNode>> children = FXCollections.observableArrayList();
            for (String child : item.getChildrenNames()) {
                children.add(new ChannelTreeItem(new ChannelTreeByPropertyNode(model, item, child)));
            }
            return children;
        }
    }

}
