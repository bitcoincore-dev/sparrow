package com.sparrowwallet.sparrow.control;

import com.sparrowwallet.drongo.wallet.WalletNode;
import com.sparrowwallet.sparrow.wallet.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.Comparator;
import java.util.List;

public class UtxosTreeTable extends CoinTreeTable {
    public void initialize(WalletUtxosEntry rootEntry) {
        getStyleClass().add("utxos-treetable");
        setBitcoinUnit(rootEntry.getWallet());

        updateAll(rootEntry);
        setShowRoot(false);

        TreeTableColumn<Entry, Entry> dateCol = new TreeTableColumn<>("Date");
        dateCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, Entry> param) -> {
            return new ReadOnlyObjectWrapper<>(param.getValue().getValue());
        });
        dateCol.setCellFactory(p -> new DateCell());
        dateCol.setSortable(true);
        getColumns().add(dateCol);

        TreeTableColumn<Entry, Entry> outputCol = new TreeTableColumn<>("Output");
        outputCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, Entry> param) -> {
            return new ReadOnlyObjectWrapper<>(param.getValue().getValue());
        });
        outputCol.setCellFactory(p -> new EntryCell());
        outputCol.setSortable(true);
        outputCol.setComparator((o1, o2) -> {
            UtxoEntry entry1 = (UtxoEntry)o1;
            UtxoEntry entry2 = (UtxoEntry)o2;
            return entry1.getDescription().compareTo(entry2.getDescription());
        });
        getColumns().add(outputCol);

        if(rootEntry.getWallet().isWhirlpoolMixWallet()) {
            TreeTableColumn<Entry, UtxoEntry.MixStatus> mixStatusCol = new TreeTableColumn<>("Mixes");
            mixStatusCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, UtxoEntry.MixStatus> param) -> {
                return ((UtxoEntry)param.getValue().getValue()).mixStatusProperty();
            });
            mixStatusCol.setCellFactory(p -> new MixStatusCell());
            mixStatusCol.setSortable(true);
            mixStatusCol.setComparator(Comparator.comparingInt(UtxoEntry.MixStatus::getMixesDone));
            getColumns().add(mixStatusCol);
        } else {
            TreeTableColumn<Entry, UtxoEntry.AddressStatus> addressCol = new TreeTableColumn<>("Address");
            addressCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, UtxoEntry.AddressStatus> param) -> {
                return ((UtxoEntry)param.getValue().getValue()).addressStatusProperty();
            });
            addressCol.setCellFactory(p -> new AddressCell());
            addressCol.setSortable(true);
            addressCol.setComparator(Comparator.comparing(o -> o.getAddress().toString()));
            getColumns().add(addressCol);
        }

        TreeTableColumn<Entry, String> labelCol = new TreeTableColumn<>("Label");
        labelCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, String> param) -> {
            return param.getValue().getValue().labelProperty();
        });
        labelCol.setCellFactory(p -> new LabelCell());
        labelCol.setSortable(true);
        getColumns().add(labelCol);

        TreeTableColumn<Entry, Number> amountCol = new TreeTableColumn<>("Value");
        amountCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<Entry, Number> param) -> {
            return new ReadOnlyObjectWrapper<>(param.getValue().getValue().getValue());
        });
        amountCol.setCellFactory(p -> new CoinCell());
        amountCol.setSortable(true);
        getColumns().add(amountCol);
        setTreeColumn(amountCol);

        setPlaceholder(getDefaultPlaceholder(rootEntry.getWallet()));
        setEditable(true);
        setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        amountCol.setSortType(TreeTableColumn.SortType.DESCENDING);
        getSortOrder().add(amountCol);

        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void updateAll(WalletUtxosEntry rootEntry) {
        setBitcoinUnit(rootEntry.getWallet());

        RecursiveTreeItem<Entry> rootItem = new RecursiveTreeItem<>(rootEntry, Entry::getChildren);
        setRoot(rootItem);
        rootItem.setExpanded(true);

        if(getColumns().size() > 0 && getSortOrder().isEmpty()) {
            TreeTableColumn<Entry, ?> amountCol = getColumns().get(getColumns().size() - 1);
            getSortOrder().add(amountCol);
            amountCol.setSortType(TreeTableColumn.SortType.DESCENDING);
        }
    }

    public void updateHistory() {
        //Utxo entries should have already been updated, so only a resort required
        if(!getRoot().getChildren().isEmpty()) {
            sort();
        }
    }

    public void updateLabel(Entry entry) {
        Entry rootEntry = getRoot().getValue();
        rootEntry.updateLabel(entry);
    }
}
