package edu.jsu.mcis.cs408.project2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Objects;

import edu.jsu.mcis.cs408.project2.databinding.FragmentPuzzleBinding;

public class PuzzleFragment extends Fragment implements TabFragment {

    private FragmentPuzzleBinding binding;

    private ArrayList<ArrayList<TextView>> gridSquareViews;
    private ArrayList<ArrayList<TextView>> gridNumberViews;

    private CrosswordViewModel model;

    private int windowHeightDp, windowWidthDp, windowOverheadDp;
    private int puzzleHeight, puzzleWidth, numberSize;

    private ConstraintSet set;

    public PuzzleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Get Shared Model Reference

        model = new ViewModelProvider(requireActivity()).get(CrosswordViewModel.class);

        // Get Puzzle Dimensions (height and width, in squares)

        puzzleHeight = Objects.requireNonNull(model.getPuzzleHeight().getValue());
        puzzleWidth = Objects.requireNonNull(model.getPuzzleWidth().getValue());

        // Initialize TextView Collections

        gridSquareViews = new ArrayList<>();
        gridNumberViews = new ArrayList<>();

        for (int i = 0; i < puzzleHeight; ++i) {

            gridSquareViews.add(new ArrayList<>());
            ArrayList<TextView> row = new ArrayList<>();

            for (int j = 0; j < puzzleWidth; ++j) {
                row.add(null);
            }

            gridNumberViews.add(row);

        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPuzzleBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Get Display Dimensions (the height, width, and tab selector overhead)

        getDisplayDimensions();

        // Create Initial View (the grid TextViews, initially filled with solid blocks)

        createGridViews();

        // Initialize/Update the View (update the on-screen puzzle from the Model)

        updateGrid();

    }

    /* Shared Event Handler for Grid Squares */

    public void onClick(View v) {


        // Get Row/Column of Tapped Square

        String[] fields = v.getTag().toString().trim().split(",");
        int row = Integer.parseInt(fields[0]);
        int column = Integer.parseInt(fields[1]);

        int box = model.getBoxNumber(row, column);

        if (!(box == 0)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle("Test Title");
            builder.setMessage("Test Message");
            final EditText input = new EditText(v.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            final String[] guessedWord = new String[1];

            builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
//                    guessedWord[0] = input.getText().toString();
                    processGuess(input.getText().toString().toUpperCase(), String.valueOf(box));

                    if (model.gameOver()) {
                        Toast.makeText(v.getContext(), "Game complete!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        }
    }

    private void processGuess(String guess, String box) {
        String keyAcross = String.valueOf(box) + WordDirection.ACROSS;
        String keyDown = String.valueOf(box) + WordDirection.DOWN;
        Word wordAcross = model.getWord(keyAcross);
        Word wordDown = model.getWord(keyDown);

        if (wordAcross != null) {
            if (guess.equals(wordAcross.getWord())) {
                model.addWordToGrid(keyAcross);
            }
        }

        if (wordDown != null) {
            if (guess.equals(wordDown.getWord())) {
                model.addWordToGrid(keyDown);
            }
        }

        updateGrid();

    }

    /* Methods for Creating Grid */

    private void createGridViews() {

        // Compute Grid Geometry (to enlarge grid to fill available space)

        int gridWidth = Math.max(puzzleHeight, puzzleWidth);

        int squareSize = ( Math.min(windowHeightDp - windowOverheadDp, windowWidthDp) / gridWidth );
        int letterSize = (int)( squareSize * 0.65 );
        numberSize = (int)( squareSize * 0.225 );

        // Get ConstraintLayout

        ConstraintLayout layout = binding.layoutPuzzle;

        // Create and Initialize Grid TextViews

        for (int i = 0; i < puzzleHeight; ++i) {

            for (int j = 0; j < puzzleWidth; ++j) {

                TextView square = new TextView(this.getContext());
                square.setId(View.generateViewId());
                square.setTag(i + "," + j);
                square.setBackground(AppCompatResources.getDrawable(binding.getRoot().getContext(), R.drawable.closed_square));
                square.setLayoutParams(new ConstraintLayout.LayoutParams(squareSize, squareSize));

                square.setTextSize(letterSize);
                square.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                square.setTextColor(Color.BLACK);
                square.setIncludeFontPadding(false);
                square.setLineSpacing(0, 0);
                square.setPadding(0, (int)(squareSize * 0.125), 0, 0);

                square.setOnClickListener(this::onClick);

                layout.addView(square);
                gridSquareViews.get(i).add(square);

            }

        }

        // Initialize Constraint Set

        set = new ConstraintSet();
        set.clone(layout);

        // Create Grid Chains (Vertical)

        int[] current = new int[puzzleHeight];

        for (int i = 0; i < puzzleHeight; ++i) {

            for (int j = 0; j < puzzleWidth; ++j) {

                int id = gridSquareViews.get(j).get(i).getId();
                current[j] = id;

            }

            set.createVerticalChain(ConstraintSet.PARENT_ID, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, current, null, ConstraintSet.CHAIN_PACKED);

        }

        // Create Grid Chains (Horizontal)

        current = new int[puzzleWidth];

        for (int i = 0; i < puzzleHeight; ++i) {

            for (int j = 0; j < puzzleWidth; ++j) {

                int id = gridSquareViews.get(i).get(j).getId();
                current[j] = id;

            }

            set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, current, null, ConstraintSet.CHAIN_PACKED);

        }

        // Apply Layout

        set.applyTo(layout);

    }

    private void getDisplayDimensions() {

        // Get display height/width, and overhead of tab selector

        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

        windowHeightDp = dm.heightPixels;
        windowWidthDp = dm.widthPixels;

        windowOverheadDp = 0;

        TypedValue tv = new TypedValue();

        if (requireActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            windowOverheadDp += TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");

        if (resourceId > 0) {
            windowOverheadDp = windowOverheadDp + windowOverheadDp + getResources().getDimensionPixelSize(resourceId);
        }

    }

    /* Methods for Updating the Grid */

    private void updateGrid() {

        // Get Grid Data from Model

        char[][] lArray = Objects.requireNonNull(model.getLetters().getValue());
        int[][] nArray = Objects.requireNonNull(model.getNumbers().getValue());

        // Update View from Model Data

        for (int i = 0; i < lArray.length; ++i) {

            for (int j = 0; j < lArray.length; ++j) {

                if (lArray[i][j] != CrosswordViewModel.BLOCK_CHAR) {
                    openSquare(i, j);
                    setSquareText(i, j, lArray[i][j]);
                }

                if (nArray[i][j] != 0) {
                    addSquareNumber(i, j, nArray[i][j]);
                }

            }

        }

    }

    public void setSquareText(int row, int column, char letter) {

        // Check if row/column is within range; if it is, place the given letter into the square

        if (row >= 0 && row < puzzleHeight && column >= 0 && column < puzzleWidth)
            gridSquareViews.get(row).get(column).setText(Character.toString(letter));

    }

    public void addSquareNumber(int row, int column, int number) {

        // Abort if row/column are out of range, or if square already has a number

        if (row >= 0 && row < puzzleHeight && column >= 0 && column < puzzleWidth && gridNumberViews != null) {

            if (gridNumberViews.get(row).get(column) == null) {

                // Get ID of square TextView at given row and column

                int square = gridSquareViews.get(row).get(column).getId();

                // Create new TextView for number; add to layout

                TextView num = new TextView(this.getContext());
                num.setId(View.generateViewId());
                num.setTextSize(numberSize);
                num.setTextColor(Color.BLACK);
                num.setText(String.valueOf(number));
                binding.layoutPuzzle.addView(num);

                // Set constraints to overlay number TextView over the corresponding square

                set.connect(num.getId(), ConstraintSet.TOP, square, ConstraintSet.TOP);
                set.connect(num.getId(), ConstraintSet.LEFT, square, ConstraintSet.LEFT, 4);
                set.connect(num.getId(), ConstraintSet.BOTTOM, square, ConstraintSet.BOTTOM);
                set.connect(num.getId(), ConstraintSet.RIGHT, square, ConstraintSet.RIGHT);

                // Add view to collection

                gridNumberViews.get(row).set(column, num);

                // Apply to Layout

                set.applyTo(binding.layoutPuzzle);

            }

        }

    }

    public void openSquare(int row, int column) {

        // Change grid square background to an open box (indicating it is part of a word)

        if (row >= 0 && row < puzzleHeight && column >= 0 && column < puzzleWidth)
            gridSquareViews.get(row).get(column).setBackground(AppCompatResources.getDrawable(binding.getRoot().getContext(), R.drawable.open_square));

    }

    @Override
    public String getTabTitle() {
        return "Puzzle";
    }

}