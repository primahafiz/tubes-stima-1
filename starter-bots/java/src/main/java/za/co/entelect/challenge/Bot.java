package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors

import java.util.*;

import static java.lang.Math.max;

public class Bot {

    private static final int maxSpeed = 9;
    private List<Integer> directionList = new ArrayList<>();

    private Random random;
    private GameState gameState;
    private Car opponent;
    private Car myCar;
    

    private final static Command FIX = new FixCommand();
    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);
    private final static Command LIZARD = new LizardCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command EMP = new EmpCommand();
    // private final static Command TWEET = new 
    private final static Command OIL = new OilCommand();
    private final static Command DECELERATE = new DecelerateCommand();
    private final static Command NOTHING = new DoNothingCommand();

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(-1);
        directionList.add(1);
    }

    public Command run() {
        int com[] = new int[11];
        com[0] = UseFix();
        com[1] = UseBoost();
        com[2] = UseAccelerate();
        com[3] = UseEMP();
        com[4] = UseLizard();
        com[5] = UseTurn_Right(myCar.boosting, myCar.boostCounter>1);     // Turn Right
        com[6] = UseTurn_Left(myCar.boosting, myCar.boostCounter>1);     // Turn Left
        com[7] = 0;     // Use Tweet
        com[8] = UseOil();
        com[9] = UseDo_Nothing();
        com[10] = UseDecelerate();


        int idxMax = 0;
        int maks = com[0];
        for(int i = 1; i < 11; i++){
            if(com[i] > maks){
                maks = com[i];
                idxMax = i;
            }
        }

        // Fungsi seleksi
        if(idxMax == 0){
            return FIX;
        }
        else if(idxMax == 1){
            return BOOST;
        }
        else if(idxMax == 2){
            return ACCELERATE;
        }
        else if(idxMax == 3){
            return EMP;
        }
        else if(idxMax == 4){
            return LIZARD;
        }
        else if(idxMax == 5){
            return TURN_RIGHT;
        }
        else if(idxMax == 6){
            return TURN_LEFT;
        }
        else if(idxMax == 7){
            return NOTHING;     // TWEET
        }
        else if(idxMax == 8){
            return OIL;
        }
        else if(idxMax == 9){
            return NOTHING;
        }
        else if(idxMax == 10){
            return DECELERATE;
        }
        else{
            return ACCELERATE;
        }
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= min(block - startBlock + myCar.speed,getMaxPos()); i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private int min(int a, int b){
        if (a < b) {
            return a;
        } else {
            return b;
        }
    }

    private boolean hasPowerUp(PowerUps a){
        for (PowerUps x:myCar.powerups){
            if(x.equals(a)){
                return true;
            }
        }
        return false;
    }

    // Mencari max speed sesuai damage dari car
    private int getMaxSpeedByDamage(){
        if(myCar.damage==5){
            return 0;
        }else if(myCar.damage==4){
            return 3;
        }else if(myCar.damage==3){
            return 6;
        }else if(myCar.damage==2){
            return 8;
        }else if(myCar.damage==1){
            return 9;
        }else{
            return 15;
        }
    }

    // Prediksi speed jika speed state bertambah sebesar numLevelUp dan jika booster aktif ataupun tidak
    // berdasarkan kondisi damage car saat ini
    private int getCurSpeed(int numLevelUp,boolean isBooster){
        if(isBooster){
            return getMaxSpeedByDamage();
        }
        int[] allSpeed={0,3,5,6,8,9,15};
        int idx=0;
        for(;idx<7;idx++){
            if(allSpeed[idx]==myCar.speed){
                break;
            }
        }
        if(idx+numLevelUp>5){
            idx=5;
        }else{
            idx=max(idx+numLevelUp,0);
        }
        
        return min(allSpeed[idx],getMaxSpeedByDamage());
    }

    private int getMaxPos(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for (int i=max(myCar.position.block-startBlock+1,0);i<=myCar.position.block-startBlock+20;i++){
            if (curLane[i]==null || curLane[i].terrain==Terrain.FINISH){
                return i;
            }
        }
        return 20;
    }


    private int UseBoost(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        int cntWallCyber=0;
        int cntMudOil=0;
        int cntBoost=0;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for(int i=max(myCar.position.block-startBlock+1,0);i<=min(getMaxPos(),myCar.position.block-startBlock+getCurSpeed(0, true));i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }else if(curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                cntMudOil++;
            }else if(curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.CYBER_TRUCK){
                cntWallCyber++;
            }else if(curLane[i].terrain==Terrain.BOOST){
                cntBoost++;
            }
        }
        if(!hasPowerUp(PowerUps.BOOST) || cntWallCyber>0){
            return 0;
        }else if(cntMudOil>0 && cntBoost==0){
            return 1;
        }else if(myCar.speed==getMaxSpeedByDamage() || (cntBoost==1 && cntMudOil>0)){
            return 2;
        }else if(cntBoost>1 && cntMudOil>0){
            return 3;
        }else if(myCar.damage==0){
            return 5;
        }else{
            return 4;
        }
    }

    private int UseAccelerate(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        int cntWallCyber=0;
        int cntMudOil=0;
        int cntBoost=0;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for(int i=max(myCar.position.block-startBlock+1,0);i<=min(getMaxPos(),myCar.position.block-startBlock+getCurSpeed(1, false));i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }else if(curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                cntMudOil++;
            }else if(curLane[i].terrain==Terrain.WALL){
                cntWallCyber++;// nama obstacle tweet apa?
            }else if(curLane[i].terrain==Terrain.BOOST){
                cntBoost++;
            }
        }
        if(myCar.speed==0){
            return 5;
        }else if(cntWallCyber>0){
            return 0;
        }else if(cntMudOil > 0 && cntBoost == 0){
            return 1;
        }else if(myCar.speed >= getCurSpeed(6,false)){
            return 2;
        }else if(cntBoost == 1 && cntMudOil>0){
            return 4;
        }else{
            return 5;
        }
    }

    private int UseDecelerate(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        boolean flagBoost=false;;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for (int i = max(myCar.position.block-startBlock+1,0); i <= min(getMaxPos(),myCar.position.block-startBlock+getCurSpeed(-1, false));i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            if(curLane[i].terrain==Terrain.BOOST){
                flagBoost=true;
                break;
            }
        }
        if(flagBoost && UseAccelerate()<4){
            return 4;
        }else{
            return 1;
        }
    }
    
    private int UseOil(){
        // check is enemy behind you
        boolean canHit = false;
        if(myCar.position.block > opponent.position.block){
            canHit = true;
        }
        // Check is there any obstacle
        Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        int obstacleDMG = 0; 
        for(int i=max(myCar.position.block-startBlock+1,0);i<=min(getMaxPos(),myCar.position.block-startBlock+myCar.speed);i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }else if(curLane[i].terrain==Terrain.WALL){
                obstacleDMG += 2;
            }
            else if(curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                obstacleDMG += 1;
            }
        }
        // Cek is power_up exist
        boolean hasPU = hasPowerUp(PowerUps.OIL);
        
        // ~Split CASE~
        if(!(hasPU && canHit)){
            return 0;
        }
        else if(hasPU && canHit && obstacleDMG > 2){
            return 1;
        }
        else if(hasPU && canHit && obstacleDMG > 0){
            return 2;
        }
        else if(hasPU && canHit && obstacleDMG == 0){
            return 3;
        }
        else if(hasPU && canHit && myCar.speed==maxSpeed && obstacleDMG > 0){
            return 4;
        }
        else if(hasPU && canHit && myCar.speed==maxSpeed && obstacleDMG == 0){
            return 5;
        }
        else{
            return 0;
        }
    }
    
    private int UseEMP(){
        int curLaneIdx = myCar.position.lane-1;
        boolean canHit = false;
        if(myCar.position.block < opponent.position.block){
            canHit = true;
        }
        if(canHit){
            for(int i = curLaneIdx-1; i <= curLaneIdx+1; i++){
                canHit = false;
                if(0<=i && i <=3){
                    if(i+1 == opponent.position.lane){
                        canHit = true;
                        break;
                    }
                }
            }
        }
        // Check is there Power_up
        boolean hasPU = hasPowerUp(PowerUps.EMP);
        // Check is there any obstacle
        Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        int obstacleDMG = 0; 
        for(int i=max(myCar.position.block-startBlock+1,0);i<=min(getMaxPos(),myCar.position.block-startBlock+myCar.speed);i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }else if(curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.TWEET){
                obstacleDMG += 2;
            }
            else if(curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                obstacleDMG += 1;
            }
        }
    
        // ~Split CASE~
        if(!(hasPU && canHit)){
            return 0;
        }
        else if(hasPU && canHit && obstacleDMG > 2){
            return 1;
        }
        else if(hasPU && canHit && obstacleDMG > 0){
            return 2;
        }
        else if(hasPU && canHit && obstacleDMG == 0){
            return 3;
        }
        else if(hasPU && canHit && myCar.speed==maxSpeed && obstacleDMG > 0){
            return 4;
        }
        else if(hasPU && canHit && myCar.speed==maxSpeed && obstacleDMG == 0 && myCar.position.lane==opponent.position.lane){
            return 5;
        }
        else{
            return 0;
        }
    }
    
    private int UseLizard(){
        // Check is there Power_up
        boolean hasPU = hasPowerUp(PowerUps.LIZARD);
        // Check is there any obstacle
        Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        int obstacleDMG = 0;
        int numPowerup=0;
        boolean safeLanding=myCar.position.block-startBlock+myCar.speed>getMaxPos()?true:(curLane[myCar.position.block-startBlock+myCar.speed].terrain!=Terrain.MUD && curLane[myCar.position.block-startBlock+myCar.speed].terrain!=Terrain.OIL_SPILL && curLane[myCar.position.block-startBlock+myCar.speed].terrain!=Terrain.WALL && curLane[myCar.position.block-startBlock+myCar.speed].terrain!=Terrain.CYBER_TRUCK);
        for(int i=max(myCar.position.block-startBlock+1,0);i<min(getMaxPos(),myCar.position.block-startBlock+myCar.speed);i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }else if(curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.CYBER_TRUCK){
                obstacleDMG += 2;
            }
            else if(curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                obstacleDMG += 1;
            }else if(curLane[i].terrain==Terrain.BOOST || curLane[i].terrain==Terrain.EMP){
                numPowerup++;
            }
        }
        int turn=max(UseTurn_Left(gameState.player.boosting, gameState.player.boostCounter>1),UseTurn_Right(gameState.player.boosting, gameState.player.boostCounter>1));
        if(!hasPU && turn>=4){
            return 0;
        }else if(!safeLanding && obstacleDMG<=2){
            return 1;
        }else if(!safeLanding && obstacleDMG>2){
            return 3;
        }else if(obstacleDMG>=2 && numPowerup>0){
            return 4;
        }else if((obstacleDMG>=2 && numPowerup==0) || obstacleDMG>3){
            return 5;
        }else{
            return 0;
        }
    }
    
    // private int UseTweet(){
    //     Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
    //     boolean hasPU = hasPowerUp(PowerUps.TWEET);
    //     if(!hasPU){
    //         return 0;
    //     }
    //     if(myCar.speed==maxSpeed){
    //         return 5;
    //     }
    
    //     return 0;
    // }
    
    // Fungsi buat ngecek apakah ada obstacle di depannya posisinya
    private boolean isNabrakObstacleInfront_atCurrentLane(int lane, int curblock, boolean isBooster, boolean isNextBooster){
        int kerusakan = myCar.damage;
        int NextSpeed;
        Lane[] curLane = gameState.lanes.get(lane-1);
        int block = gameState.lanes.get(0)[0].position.block;
        for (int i = max(curblock - block, 0); i <= min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster)); i++) {
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                kerusakan += 1;
            }
            else if (curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.CYBER_TRUCK){
                kerusakan += 2;
            }
        }
        // Kecepatan mobil di next round ketika setelah satu round
        if (kerusakan == 5){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 0);
        }
        else if (kerusakan == 4){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 3);
        }
        else if (kerusakan == 3){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 6);
        }
        else if (kerusakan == 2){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 8);
        }
        else if (kerusakan == 1){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 9);
        }
        else{
            NextSpeed = min(getCurSpeed(0, isNextBooster), 15);
        }
        // Buat cek apakah ada obstacle yang bakal ketabrak di depan bloknya
        for (int i = curblock - block + getCurSpeed(0, isBooster) + 1; i <=min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster) + NextSpeed); i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL || curLane[i].terrain==Terrain.WALL){
                return true;
            }
        }
        return false;
    }

    // Fungsi untuk mengecek apakah bakal nabrak pada posisi tersebut
    private boolean isNabrakObstacle_turning(int lane, int curblock, boolean isBooster){
        Lane[] curLane=gameState.lanes.get(lane-1);
        int block = gameState.lanes.get(0)[0].position.block;
        for (int i = max(curblock - block, 0); i <= min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster)); i++) {
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL || curLane[i].terrain==Terrain.WALL){
                return true;
            }
        }
        return false;
    }

    // Fungsi buat ngecek apakah ada PowerUp di depannya posisinya
    private boolean isNabrakPowerUpInfront_atCurrentLane(int lane, int curblock, boolean isBooster, boolean isNextBooster){
        int kerusakan = myCar.damage;
        int NextSpeed;
        Lane[] curLane = gameState.lanes.get(lane-1);
        int block = gameState.lanes.get(0)[0].position.block;
        for (int i = max(curblock - block, 0); i <= min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster)); i++) {
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL){
                kerusakan += 1;
            }
            else if (curLane[i].terrain==Terrain.WALL){
                kerusakan += 2;
            }
        }
        // Kecepatan mobil di next round ketika setelah satu round
        if (kerusakan == 5){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 0);
        }
        else if (kerusakan == 4){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 3);
        }
        else if (kerusakan == 3){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 6);
        }
        else if (kerusakan == 2){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 8);
        }
        else if (kerusakan == 1){
            NextSpeed = min(getCurSpeed(0, isNextBooster), 9);
        }
        else{
            NextSpeed = min(getCurSpeed(0, isNextBooster), 15);
        }
        // Buat cek apakah ada obstacle yang bakal ketabrak di depan bloknya
        for (int i = curblock - block + getCurSpeed(0, isBooster) + 1; i <=min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster) + NextSpeed); i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.OIL_POWER || curLane[i].terrain==Terrain.BOOST || curLane[i].terrain==Terrain.EMP || curLane[i].terrain==Terrain.TWEET || curLane[i].terrain==Terrain.LIZARD){
                return true;
            }
        }
        return false;
    }

    // Fungsi untuk mengecek apakah bakal nabrak power up atua tidak pada posisi tersebut
    private boolean isNabrakpowerUp_turning(int lane, int curblock, boolean isBooster){
        Lane[] curLane=gameState.lanes.get(lane-1);
        int block = gameState.lanes.get(0)[0].position.block;
        for (int i = max(curblock - block, 0); i <= min(getMaxPos(),curblock - block + getCurSpeed(0, isBooster)); i++) {
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.OIL_POWER || curLane[i].terrain==Terrain.BOOST || curLane[i].terrain==Terrain.EMP || curLane[i].terrain==Terrain.TWEET || curLane[i].terrain==Terrain.LIZARD){
                return true;
            }
        }
        return false;
    }

    // Fungsi buat coommand Turn_Left
    private int UseTurn_Left(boolean isBooster, boolean isNextBooster){
        if (myCar.position.lane - 1 > 0){
            if (isNabrakpowerUp_turning(myCar.position.lane-1, myCar.position.block, isBooster) || isNabrakpowerUp_turning(myCar.position.lane, myCar.position.block, isBooster) || isNabrakPowerUpInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                    return 5;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                    return 4;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster)){
                    return 1;
                }
                else if (!isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster)){
                    return 2;
                }
                else{
                    return 0;
                }
            }
            else{
                if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                    return 4;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                    return 3;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster)){
                    return 0;
                }
                else if (!isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane-1, myCar.position.block, isBooster)){
                    return 1;
                }
                else{
                    return 0;
                }
            }
        }
        else{
            return 0;
        }
    }

    //Fungsi buat command Turn_Right
    private int UseTurn_Right(boolean isBooster, boolean isNextBooster){
        if (myCar.position.lane - 1 < 3){
            if (isNabrakpowerUp_turning(myCar.position.lane+1, myCar.position.block, isBooster) || isNabrakpowerUp_turning(myCar.position.lane, myCar.position.block, isBooster) || isNabrakPowerUpInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                    return 5;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                    return 4;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster)){
                    return 1;
                }
                else if (!isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster)){
                    return 2;
                }
                else{
                    return 0;
                }
            }
            else{
                if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                    return 4;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                    return 3;
                }
                else if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster)){
                    return 0;
                }
                else if (!isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrakObstacle_turning(myCar.position.lane+1, myCar.position.block, isBooster)){
                    return 1;
                }
                else{
                    return 0;
                }
            }
        }
        else{
            return 0;
        }
    }

    // Fungsi buat command Fix
    private int UseFix(){
        if (myCar.damage >= 3 || (myCar.damage >= 2 && hasPowerUp(PowerUps.BOOST)) && getMaxPos()>myCar.speed){
            return 5;
        }
        else if (myCar.damage >= 2){
            return 4;
        }
        else if (myCar.damage >= 1){
            return 3;
        }
        else {
            return 0;
        }
    }

    // Fungsi buat command do nothing
    private int UseDo_Nothing(){
        if (isNabrakObstacle_turning(myCar.position.lane, myCar.position.block, myCar.boosting) || hasPowerUp(PowerUps.BOOST) || hasPowerUp(PowerUps.OIL) || hasPowerUp(PowerUps.TWEET) || hasPowerUp(PowerUps.LIZARD) || hasPowerUp(PowerUps.EMP)){
            return 0;
        }
        else if (!hasPowerUp(PowerUps.BOOST) && !hasPowerUp(PowerUps.OIL) && !hasPowerUp(PowerUps.TWEET) && !hasPowerUp(PowerUps.LIZARD) && !hasPowerUp(PowerUps.EMP)){
            return 1;
        }
        else {
            return 2;
        }
    }
}
