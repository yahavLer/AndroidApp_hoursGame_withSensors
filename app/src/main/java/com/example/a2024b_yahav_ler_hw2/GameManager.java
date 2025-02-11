package com.example.a2024b_yahav_ler_hw2;

import static android.content.ContentValues.TAG;


import static com.example.a2024b_yahav_ler_hw2.R.drawable.grass;
import static com.example.a2024b_yahav_ler_hw2.R.drawable.horse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Random;
import android.Manifest;

public class GameManager extends AppCompatActivity implements CallBack_List{
    private AppCompatImageButton zoo_left;
    private AppCompatImageButton zoo_right;
    private ImageView[][] zoo_animals;
    private ImageView[] zoo_live;
    private int grassImages = R.drawable.grass;
    private int farmerPosCol;
    private int farmerPosRow;
    private boolean isGameOver = false;
    private boolean loop;
    private int amountRow, amountColl;
    private int delay = 1000;
    private int fastDelay = 500;
    private int slowDelay = 1500;
    private int numLives = 3;
    private final Handler handler = new Handler();
    private Runnable runnable;
    private MoveDetector moveDetector;
    private Context context;
    private AppCompatActivity activity;
    private boolean gameSensors;
    private TextView numScore;
    private int score = 0; // משתנה ניקוד
    private final int SCORE_INTERVAL = 5000; // כל כמה זמן להוסיף נקודות (מילישניות)
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SoundPlayer soundPlayer;
    private int time=0;
    private ScoreManager scoreManager;
    private Location currentLocation;



    public GameManager() {
        super();
    }

    public GameManager(Context context, AppCompatActivity gameActivity, boolean gameSensors) {
        this.context = context;
        this.activity = gameActivity;
        this.gameSensors = gameSensors;
        this.soundPlayer = new SoundPlayer(gameActivity);
        this.scoreManager = new ScoreManager(context);
    }
    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    public void startGame(boolean useSensors) {
        if (useSensors) {
            initMoveDetector();
        } else {
            initButton();
        }
        this.runnable = new Runnable() {
            public void run() {
                if (!isGameOver) {
                    moveHorse();
                    time++;
                    score += 2;
                    numScore.setText(String.valueOf(score));
                    if (time%3==0){
                        addGrass();
                    }
                } else {
                    return;
                }
                handler.postDelayed(runnable, delay);
            }
        };
        start();
    }


    private void start() {
        handler.postDelayed(runnable, delay);
    }


    private void initMoveDetector() {
        moveDetector = new MoveDetector(context, new MoveCallback() {
            @Override
            public void moveRight() {
                if (moveDetector.getTiltRightCount() > 0) {
                    moveFarmerRight();
                }
            }

            @Override
            public void moveLeft() {
                if (moveDetector.getTiltLeftCount() > 0) {
                    moveFarmerLeft();
                }
            }

//            @Override
//            public void moveBackward() {
//                if (moveDetector.getTiltBackwardCount() > 0)
//                    setSpeed(slowDelay);
//            }
//
//            @Override
//            public void moveForward() {
//                if (moveDetector.getTiltForwardCount() > 0)
//                    setSpeed(fastDelay);
//            }
        });
    }

    private void initButton() {
        zoo_right.setOnClickListener(v -> moveFarmerRight());
        zoo_left.setOnClickListener(v -> moveFarmerLeft());
    }

    private void moveHorse() {
        checkLives();
        Random random = new Random();
        int num;
        for (int i = zoo_animals.length - 3; i >= 0; i--) {
            for (int j = 0; j < zoo_animals[i].length; j++) {
                if (i == zoo_animals.length - 3) {
                    zoo_animals[i + 1][j].setVisibility(View.INVISIBLE);
                }
                if (zoo_animals[i][j].getVisibility() == View.VISIBLE) {
                    Drawable drawable = zoo_animals[i][j].getDrawable();
                    zoo_animals[i][j].setVisibility(View.INVISIBLE);

                    if (drawable != null && drawable.getConstantState() != null) {
                        if (drawable.getConstantState().equals(ResourcesCompat.getDrawable(context.getResources(), grassImages, null).getConstantState())) {
                            zoo_animals[i + 1][j].setImageResource(grassImages);
                            zoo_animals[i + 1][j].setVisibility(View.VISIBLE);
                        } else if (drawable.getConstantState().equals(ResourcesCompat.getDrawable(context.getResources(), horse, null).getConstantState())) {
                            zoo_animals[i + 1][j].setImageResource(horse);
                            zoo_animals[i + 1][j].setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            if (i == 0) {
                num = random.nextInt(5);
                zoo_animals[0][num].setVisibility(View.VISIBLE);
                zoo_animals[0][num].setImageResource(horse);
            }
        }
    }


    private boolean isGrassImage(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        Drawable grassDrawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.grass, null);

        return drawable != null && grassDrawable != null && drawable.getConstantState().equals(grassDrawable.getConstantState());
    }

    public void addGrass() {
        Random rand = new Random();
        int lane = rand.nextInt(zoo_animals[0].length);
        zoo_animals[0][lane].setImageResource(grass);
    }

    public void checkLives() {
        if (numLives == 0 && !isGameOver) {
            isGameOver = true;
            lose();
        } else {
            checkPlace();
        }
    }

    //////// saved score ///////////
    public void lose() {
        zoo_left.setEnabled(false);
        zoo_right.setEnabled(false);
        openAdvertisementDialog();
    }

    public void openAdvertisementDialog() {
        new MaterialAlertDialogBuilder(context).setTitle("No lives")
                .setMessage("your score: " + score +"\n Do you want to save your score?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    openSaveScoreDialog(score, this);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    openContinueDialog();
                })
                .show();
    }

    public void openContinueDialog() {
        new MaterialAlertDialogBuilder(context).setTitle("play again?")
                .setMessage("Do you want to play again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    continueGame();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    gameDone();
                })
                .show();
    }

    public void openSaveScoreDialog(int score, CallBack_List callBackList) {
        final EditText input = new EditText(context);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Enter your name")
                .setView(input)
                .setPositiveButton("Save", (dialog, whichButton) -> {
                    String playerName = input.getText().toString();
                    callBackList.addPlayer(playerName);
                    Toast.makeText(context, "Score saved!", Toast.LENGTH_SHORT).show();
                    gameDone();
                })
                .show(); // ensure the dialog is shown
    }

    @Override
    public void addPlayer(String user) {
        if (currentLocation != null) {
            scoreManager.addRecord(user, score, currentLocation.getLatitude(), currentLocation.getLongitude());
        } else {
            scoreManager.addRecord(user, score, 0, 0);
        }
    }

    private void checkPlace() {
        if (zoo_animals[farmerPosRow-1][farmerPosCol].getVisibility() == View.VISIBLE) {
            if (isGrassImage(zoo_animals[farmerPosRow-1][farmerPosCol])){
                makeSoundGrass();
                score+=10;
                numScore.setText(String.valueOf(score));
            }
            else{
                numLives--;
                Log.d(TAG, "numLives: "+ numLives);
                updateLive();
                vibrate();
                makeSoundCrash();
            }
        }
    }

    public void gameDone() {
        stopGame();
        Toast.makeText(context, "You lose", Toast.LENGTH_SHORT).show();
        Log.d("pttt", "Game Done");
        zoo_left.setEnabled(false);
        zoo_right.setEnabled(false);
        activity.finish();
    }

    public void stopGame() {
        handler.removeCallbacks(runnable);
    }

    public void continueGame() {
        numLives = 3;
        score = 0;
        numScore.setText(String.valueOf(score));
        updateLive();
        isGameOver = false;
        zoo_left.setEnabled(true);
        zoo_right.setEnabled(true);
        start();
    }

    private void updateLive() {
        int amountLive=zoo_live.length;
        for (int i = 0; i < amountLive; i++) {
            zoo_live[i].setVisibility(View.VISIBLE);
        }
        if (numLives<3){
            int removeLive= zoo_live.length - numLives;
            for (int i = 0; i <removeLive ; i++) {
                zoo_live[(amountLive - i)-1].setVisibility(View.INVISIBLE);
            }
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }

    private void makeSoundCrash() {
        loop = false;
        soundPlayer.playSound(R.raw.horsecrash, loop);
    }

    private void makeSoundGrass() {
        loop = false;
        soundPlayer.playSound(R.raw.grass, loop);
    }

    public void moveFarmerRight() {
        if (farmerPosCol < amountColl - 1) {
            zoo_animals[farmerPosRow][farmerPosCol].setVisibility(View.INVISIBLE);
            farmerPosCol++;
            zoo_animals[farmerPosRow][farmerPosCol].setVisibility(View.VISIBLE);
        }
    }

    public void moveFarmerLeft() {
        if (farmerPosCol > 0) {
            zoo_animals[farmerPosRow][farmerPosCol].setVisibility(View.INVISIBLE);
            farmerPosCol--;
            zoo_animals[farmerPosRow][farmerPosCol].setVisibility(View.VISIBLE);
        }
    }

    public void setSpeed(int speed) {
        Toast toast;
        if (speed == slowDelay) {
            delay = slowDelay;  // Increase speed
            toast = Toast.makeText(context, "slower", Toast.LENGTH_SHORT);
            new Handler().postDelayed(toast::cancel, 500); // Cancel the toast after 500 milliseconds
        } else if (speed == fastDelay) {
            delay = fastDelay; // Decrease speed
            toast = Toast.makeText(context, "faster", Toast.LENGTH_SHORT);
            new Handler().postDelayed(toast::cancel, 500); // Cancel the toast after 500 milliseconds
        } else {
            delay = 1000; // Normal speed
            toast = Toast.makeText(context, "regular", Toast.LENGTH_SHORT);
            new Handler().postDelayed(toast::cancel, 500); // Cancel the toast after 500 milliseconds
        }
        toast.show();
    }

    public void findViews() {
        this.zoo_left = activity.findViewById(R.id.zoo_left);
        this.zoo_right = activity.findViewById(R.id.zoo_right);
        this.numScore = activity.findViewById(R.id.num_score);
        this.zoo_animals = new AppCompatImageView[][]{
                {activity.findViewById(R.id.horse01), activity.findViewById(R.id.horse02), activity.findViewById(R.id.horse03), activity.findViewById(R.id.horse04), activity.findViewById(R.id.horse05)},
                {activity.findViewById(R.id.horse06), activity.findViewById(R.id.horse07), activity.findViewById(R.id.horse08), activity.findViewById(R.id.horse09), activity.findViewById(R.id.horse10)},
                {activity.findViewById(R.id.horse11), activity.findViewById(R.id.horse12), activity.findViewById(R.id.horse13), activity.findViewById(R.id.horse14), activity.findViewById(R.id.horse15)},
                {activity.findViewById(R.id.horse16), activity.findViewById(R.id.horse17), activity.findViewById(R.id.horse18), activity.findViewById(R.id.horse19), activity.findViewById(R.id.horse20)},
                {activity.findViewById(R.id.horse21), activity.findViewById(R.id.horse22), activity.findViewById(R.id.horse23), activity.findViewById(R.id.horse24), activity.findViewById(R.id.horse25)},
                {activity.findViewById(R.id.horse26), activity.findViewById(R.id.horse27), activity.findViewById(R.id.horse28), activity.findViewById(R.id.horse29), activity.findViewById(R.id.horse30)},
                {activity.findViewById(R.id.horse31), activity.findViewById(R.id.horse32), activity.findViewById(R.id.horse33), activity.findViewById(R.id.horse34), activity.findViewById(R.id.horse35)},
                {activity.findViewById(R.id.horse36), activity.findViewById(R.id.horse37), activity.findViewById(R.id.horse38), activity.findViewById(R.id.horse39), activity.findViewById(R.id.horse40)},
                {activity.findViewById(R.id.horse41), activity.findViewById(R.id.horse42), activity.findViewById(R.id.horse43), activity.findViewById(R.id.horse44), activity.findViewById(R.id.horse45)},
                {activity.findViewById(R.id.farmer1), activity.findViewById(R.id.farmer2), activity.findViewById(R.id.farmer3), activity.findViewById(R.id.farmer4), activity.findViewById(R.id.farmer5)}
        };
        this.zoo_live = new AppCompatImageView[]{
                activity.findViewById(R.id.live1),
                activity.findViewById(R.id.live2),
                activity.findViewById(R.id.live3),
        };
        this.amountRow = zoo_animals.length;
        this.amountColl = zoo_animals[0].length;
        this.farmerPosRow = amountRow - 1;
        this.farmerPosCol = amountColl / 2;
    }

    public void ButtonUnVisibility() {
        zoo_left.setVisibility(View.GONE);
        zoo_right.setVisibility(View.GONE);
    }

    public void ButtonVisibility() {
        zoo_left.setVisibility(View.VISIBLE);
        zoo_right.setVisibility(View.VISIBLE);
    }

    public void initializeHorses() {
        int pos=0;
        for (int i = 0; i < amountRow - 1; i++) {
            for (int j = 0; j < amountColl; j++) {
                if ((i%2)!=0 ) {
                    zoo_animals[i][j].setVisibility(View.INVISIBLE);
                } else if((i%2)==0 && (j%2)==0 && (pos==0)) {
                    zoo_animals[i][j].setVisibility(View.INVISIBLE);
                    pos=1;
                } else if((i%2)==0 && (j%2)!=0 && (pos==1)) {
                    zoo_animals[i][j].setVisibility(View.INVISIBLE);
                    pos=0;
                }
            }
        }
        zoo_animals[farmerPosRow][farmerPosCol].setVisibility(View.VISIBLE);
    }

    public MoveDetector getMoveDetector(){
        return moveDetector;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moveDetector != null) {
            moveDetector.stop();
        }
        stopGame();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (moveDetector != null) {
            moveDetector.stop();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (moveDetector != null) {
            moveDetector.start();
        }
    }

}
