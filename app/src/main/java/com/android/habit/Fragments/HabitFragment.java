package com.android.habit.Fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.habit.Adapters.TasksAdapter;
import com.android.habit.Databases.TasksDB;
import com.android.habit.Interfaces.DatePickable;
import com.android.habit.Objects.SwipeDetector;
import com.android.habit.Objects.Task;
import com.android.habit.StaticObjects.ProgressManager;
import com.android.habit.StaticObjects.DaysManager;
import com.android.habit.StaticObjects.TasksList;
import com.android.habit.R;
import com.android.habit.StaticObjects.TasksManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Oscar_Local on 6/14/2016.
 */
public class HabitFragment extends Fragment {

    //List variables
    ArrayAdapter<Task> tasksAdapter;
    ListView listView;
    AdapterView.OnItemClickListener itemClickListener;
    View.OnClickListener onClickListener;
    SwipeDetector swipeDetector;

    TasksDB db;

    //Controls
    Button addTaskButton;
    ProgressBar levelBar;
    TextView levelText;

    //Dialog data
    Dialog addTaskDialog;

    //Static variables
    static String newTaskName;
    static String newTaskDescription;
    static int newTaskPoints;
    static long newTaskTime;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        tasksAdapter = new TasksAdapter(this.getActivity(), (ArrayList<Task>)(TasksList.getList()), this);
        TasksManager.addNewAdapter(tasksAdapter);

        db = new TasksDB(this.getActivity());

        //Register listeners
        itemClickListener = new HabitsItemClickListener();
        onClickListener = new HabitsClickListener();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_habit, container, false);

        //Setting up dialog
        addTaskDialog = new AddTaskDialog(getActivity()).getNewDialog();


        //Setting up listview
        swipeDetector = new SwipeDetector();
        listView = (ListView) v.findViewById(R.id.fragment_habit_listview);
        listView.setOnTouchListener(swipeDetector);
        listView.setOnItemClickListener(itemClickListener);
        listView.setAdapter((ArrayAdapter) tasksAdapter);
        tasksAdapter.notifyDataSetChanged();

        //Controls
        addTaskButton = (Button) v.findViewById(R.id.fragment_habit_button_add_task);
        addTaskButton.setOnClickListener(onClickListener);
        levelText = (TextView) v.findViewById(R.id.fragment_habit_textview_level_number);
        levelBar = (ProgressBar) v.findViewById(R.id.fragment_habit_level_bar);
        levelBar.setProgress(ProgressManager.getCurrentProgress());
        levelText.setText(String.valueOf(ProgressManager.getCurrentLevel()));


        //Update tasks
        if(DaysManager.isNextDay()) {
            clearTodayTasks();
        }
        DaysManager.markTodayVisited();



        return v;
    }

    public void completeTask(Task task, int position) {
        db.getThisDaysOverdueTasks(DaysManager.getTodayAsLong());
        removeTask(position);
        updateProgressFromTask(task, true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }


    /*=========================================================================
                            Private helper methods
    =========================================================================*/


    private void showNewTaskDialog() {
        new AddTaskDialog(getActivity()).getNewDialog().show();
    }

    private void updateProgressFromTask(Task task, boolean success) {
        if(success) {
            ProgressManager.updateLevelProgress(task.getTaskPoints());
        }
        else {
            ProgressManager.updateLevelProgress(-3*task.getTaskPoints());
        }
        levelBar.setProgress(ProgressManager.getCurrentProgress());
        levelText.setText(String.valueOf(ProgressManager.getCurrentLevel()));
    }

    private void addNewTask() {
        TasksManager.addNewTask(new Task(newTaskName, newTaskDescription, newTaskPoints, newTaskTime));
        wipeStaticDialogData();
    }

    private void removeTask(int pos) {TasksManager.removeTask(TasksList.getList().get(pos));}


    private void wipeStaticDialogData() {
        newTaskName = "";
        newTaskDescription = "";
        newTaskPoints = 0;
        newTaskTime = 0;
    }

    private void clearTodayTasks() {
        for(Task task : db.getThisDaysOverdueTasks(DaysManager.getTodayAsLong())) {
            updateProgressFromTask(task, false);
        }
        TasksManager.moveAllTodaysTasksToNextDay();
    }

    /*=========================================================================
                                Inner classes
     ========================================================================*/
    protected class HabitsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.fragment_habit_button_add_task:
                    showNewTaskDialog();
                    break;
            }
        }
    }
    protected class HabitsItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(swipeDetector.swipeDetected()) {
                if(swipeDetector.getAction() == SwipeDetector.Action.RL) {
                    View taskView = tasksAdapter.getView(position, view, parent);
                    taskView.findViewById(R.id.listitem_task_button_complete).setVisibility(View.VISIBLE);
                }
                else if(swipeDetector.getAction() == SwipeDetector.Action.LR) {
                    View taskView = tasksAdapter.getView(position, view, parent);
                    taskView.findViewById(R.id.listitem_task_button_complete).setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    protected class DateSelectorDialog implements DatePickerDialog.OnDateSetListener {
        DatePickerDialog dialog;
        Context context;
        DatePickable owner;
        GregorianCalendar currentDateCal;
        GregorianCalendar dateSelectorCal;

        public DateSelectorDialog(Context context, DatePickable owner) {
            this.context = context;
            dateSelectorCal = new GregorianCalendar();
            dateSelectorCal.setTime(DaysManager.convertIsolateDay(dateSelectorCal.getTime()));
            currentDateCal = new GregorianCalendar();
            currentDateCal.setTime(DaysManager.convertIsolateDay(currentDateCal.getTime()));
            this.owner = owner;
            newTaskTime = dateSelectorCal.getTime().getTime();
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            dateSelectorCal.set(Calendar.YEAR, year);
            dateSelectorCal.set(Calendar.MONTH, month);
            dateSelectorCal.set(Calendar.DAY_OF_MONTH, day);
            newTaskTime = dateSelectorCal.getTime().getTime();
            owner.updateDate();

        }

        public DatePickerDialog getNewDialog() {
            dialog = new DatePickerDialog(context, this, dateSelectorCal.get(Calendar.YEAR), dateSelectorCal.get(Calendar.MONTH), dateSelectorCal.get(Calendar.DAY_OF_MONTH)) {
                @Override
                public void onDateChanged(DatePicker view, int year, int month, int day) {
                    GregorianCalendar selectedCal = new GregorianCalendar();
                    selectedCal.set(year, month, day, 0, 0);
                    if(selectedCal.compareTo(currentDateCal) == -1) {
                        view.updateDate(currentDateCal.get(Calendar.YEAR), currentDateCal.get(Calendar.MONTH), currentDateCal.get(Calendar.DAY_OF_MONTH));
                    }
                }
            };
            dialog.getDatePicker().setMinDate(currentDateCal.getTimeInMillis());
            dialog.setTitle("Add Date");
            return dialog;
        }
    }

    protected class AddTaskDialog implements DatePickable{
        Dialog dialog;
        DateSelectorDialog dateSelector;
        Context context;
        public AddTaskDialog(Context context) {
            this.context = context;
            dateSelector = new DateSelectorDialog(context, this);
        }

        public Dialog getNewDialog() {
            dialog = new Dialog(context);
            dialog.setTitle("Add New Task");
            dialog.setContentView(R.layout.fragment_habit_dialog);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                }
            });

            updateDate();

            final EditText nameEditor = (EditText) dialog.findViewById(R.id.fragment_habit_dialog_edittext_name);
            final EditText descriptionEditor = (EditText) dialog.findViewById(R.id.fragment_habit_dialog_edittext_description);
            final EditText pointsEditor = (EditText) dialog.findViewById(R.id.fragment_habit_dialog_edittext_points);
            final EditText dateEditor = (EditText) dialog.findViewById(R.id.fragment_habit_dialog_edittext_date);
            Button okButton = (Button) dialog.findViewById(R.id.fragment_habit_dialog_button_ok);
            Button cancelButton = (Button) dialog.findViewById(R.id.fragment_habit_dialog_button_cancel);

            dateEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus) dateSelector.getNewDialog().show();
                }
            });
            dateEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dateSelector.getNewDialog().show();
                }
            });

            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newTaskName = nameEditor.getText().toString();
                    newTaskDescription = descriptionEditor.getText().toString();
                    if(pointsEditor.getText().toString().equals("")) {
                        newTaskPoints = 0;
                    }
                    else {
                        newTaskPoints = Integer.parseInt(pointsEditor.getText().toString());
                    }

                    if(!newTaskName.equals("")) {
                        addNewTask();
                        dialog.cancel();
                    }
                    else {
                        Toast.makeText(context, "Task must be assigned a name.", Toast.LENGTH_LONG).show();
                    }
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                }
            });

            return dialog;
        }

        @Override
        public void updateDate() {
            final EditText dateEditor = (EditText) dialog.findViewById(R.id.fragment_habit_dialog_edittext_date);
            dateEditor.setText(DaysManager.getFriendlyDateString(newTaskTime));
        }
    }

}
