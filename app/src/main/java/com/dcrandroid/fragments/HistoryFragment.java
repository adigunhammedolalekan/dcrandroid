package com.dcrandroid.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.activities.TransactionDetailsActivity;
import com.dcrandroid.adapter.TransactionAdapter;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.RecyclerTouchListener;
import com.dcrandroid.util.TransactionSorter;
import com.dcrandroid.util.TransactionsResponse;
import com.dcrandroid.data.Transaction;
import com.dcrandroid.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import mobilewallet.GetTransactionsResponse;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class HistoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, GetTransactionsResponse {
    private List<Transaction> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private  TextView refresh;
    private SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    private DcrConstants constants;
    //    private View progressContainer;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constants = DcrConstants.getInstance();
        View rootView = inflater.inflate(R.layout.content_history, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        swipeRefreshLayout = rootView.getRootView().findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary,
                R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this);
//        progressContainer = rootView.findViewById(R.id.progressContainers);
        refresh = rootView.getRootView().findViewById(R.id.no_history);
        transactionAdapter = new TransactionAdapter(transactionList, layoutInflater);
        recyclerView = rootView.getRootView().findViewById(R.id.history_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Transaction history = transactionList.get(position);
                Intent i = new Intent(getContext(), TransactionDetailsActivity.class);
                i.putExtra(Constants.EXTRA_AMOUNT,history.getAmount());
                i.putExtra(Constants.EXTRA_TRANSACTION_FEE,history.getTransactionFee());
                i.putExtra(Constants.EXTRA_TRANSACTION_DATE,history.getTxDate());
                i.putExtra(Constants.EXTRA_BLOCK_HEIGHT, history.getHeight());
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_INPUT, history.totalInput);
                i.putExtra(Constants.EXTRA_TRANSACTION_TOTAL_OUTPUT, history.totalOutput);
                i.putExtra(Constants.EXTRA_TRANSACTION_TYPE,history.getType());
                i.putExtra(Constants.EXTRA_TRANSACTION_HASH, history.getHash());
                i.putExtra(Constants.EXTRA_TRANSACTION_DIRECTION, history.getDirection());
                i.putStringArrayListExtra(Constants.EXTRA_TRANSACTION_INPUTS,history.getUsedInput());
                i.putStringArrayListExtra(Constants.EXTRA_TRANSACTION_OUTPUTS,history.getWalletOutput());
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(transactionAdapter);
        registerForContextMenu(recyclerView);
        prepareHistoryData();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle(getString(R.string.history));
    }

    private void prepareHistoryData(){
        swipeRefreshLayout.setRefreshing(true);
        loadTransactions();
        transactionList.clear();
        new Thread(){
            public void run(){
                try {
                    constants.wallet.getTransactions(HistoryFragment.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void saveTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            file.createNewFile();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(transactionList);
            objectOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadTransactions(){
        try {
            File path = new File(getContext().getFilesDir()+"/savedata/");
            path.mkdirs();
            File file = new File(getContext().getFilesDir()+"/savedata/transactions");
            if(file.exists()){
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
                List<Transaction> temp = (List<Transaction>) objectInputStream.readObject();
                transactionList.addAll(temp);
                transactionAdapter.notifyDataSetChanged();
                System.out.println("Done: "+transactionList.size());
                if(transactionList.size() == 0){
                    if(refresh.isShown()){
                        refresh.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRefresh() {
        prepareHistoryData();
    }

    @Override
    public void onResult(String s) {
        if(getActivity() == null){
            return;
        }
        TransactionsResponse response = TransactionsResponse.parse(s);
        if(response.errorOccurred){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!refresh.isShown()){
                        refresh.setVisibility(View.VISIBLE);
                    }
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }else if(response.transactions.size() == 0){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!refresh.isShown()){
                        refresh.setVisibility(View.VISIBLE);
                    }
                    recyclerView.setVisibility(View.GONE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            });
        }else {
            final List<Transaction> temp = new ArrayList<>();
            for (int i = 0; i < response.transactions.size(); i++) {
                Transaction transaction = new Transaction();
                TransactionsResponse.TransactionItem item = response.transactions.get(i);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(item.timestamp * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat(" dd yyyy, hh:mma",Locale.getDefault());
                transaction.setTxDate(calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT,Locale.getDefault()) + sdf.format(calendar.getTime()).toLowerCase());
                transaction.setTime(item.timestamp);
                transaction.setTransactionFee(item.fee);
                transaction.setType(item.type);
                transaction.setHash(item.hash);
                transaction.setHeight(item.height);
                transaction.setDirection(item.direction);
                transaction.setAmount(item.amount);
                ArrayList<String> usedInput = new ArrayList<>();
                for (int j = 0; j < item.debits.size(); j++) {
                    transaction.totalInput += item.debits.get(j).previous_amount;
                    usedInput.add(item.debits.get(j).accountName + "\n" + Utils.formatDecred(item.debits.get(j).previous_amount));
                }
                ArrayList<String> output = new ArrayList<>();
                for (int j = 0; j < item.credits.size(); j++) {
                    transaction.totalOutput += item.credits.get(j).amount;
                    output.add(item.credits.get(j).address + "\n" + Utils.formatDecred(item.credits.get(j).amount));
                }
                transaction.setUsedInput(usedInput);
                transaction.setWalletOutput(output);
                temp.add(transaction);
            }
            if(getActivity() == null){
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Collections.sort(temp, new TransactionSorter());
                    transactionList.clear();
                    transactionList.addAll(0,temp);
                    if(refresh.isShown()){
                        refresh.setVisibility(View.INVISIBLE);
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    if(swipeRefreshLayout.isRefreshing()){
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    transactionAdapter.notifyDataSetChanged();
                    saveTransactions();
                }
            });
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(Constants.ACTION_BLOCK_SCAN_COMPLETE)){
                prepareHistoryData();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("History OnPause");
        if(getActivity() != null){
            getActivity().unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("History OnResume");
        if(getActivity() != null) {
            IntentFilter filter = new IntentFilter(Constants.ACTION_BLOCK_SCAN_COMPLETE);
            getActivity().registerReceiver(receiver, filter);
        }
    }
}