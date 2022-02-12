package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

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

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(-1);
        directionList.add(1);
    }

    public Command run() {
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        if (myCar.damage >= 5) {
            return new FixCommand();
        }
        if (blocks.contains(Terrain.MUD)) {
            int i = random.nextInt(directionList.size());
            return new ChangeLaneCommand(directionList.get(i));
        }
        return new AccelerateCommand();
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
        for (int i = max(block - startBlock, 0); i <= block - startBlock + myCar.speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private int min(int a,int b){
        if(a<b){
            return a;
        }else{
            return b;
        }
    }

    private boolean hasPowerUp(PowerUps a){
        for(PowerUps x:myCar.powerups){
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


    private int UseBoost(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        int cntWallCyber=0;
        int cntMudOil=0;
        int cntBoost=0;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for(int i=max(myCar.position.block-startBlock+1,0);i<=myCar.position.block-startBlock+getCurSpeed(0, true);i++){
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
        if(!hasPowerUp(PowerUps.BOOST) || cntWallCyber>0){
            return 0;
        }else if(cntMudOil>0 && cntBoost==0){
            return 1;
        }else if(myCar.speed==getMaxSpeedByDamage() || (cntBoost==1 && cntMudOil>0)){
            return 2;
        }else if(cntBoost>1 && cntMudOil>0){
            return 3;
        }else{
            return 5;
        }
    }

    private int UseAccelerate(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        int cntWallCyber=0;
        int cntMudOil=0;
        int cntBoost=0;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for(int i=max(myCar.position.block-startBlock+1,0);i<=myCar.position.block-startBlock+getCurSpeed(1, false);i++){
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
        if(cntWallCyber>0){
            return 0;
        }else if(cntMudOil>0 && cntBoost==0){
            return 1;
        }else if(myCar.speed>=getCurSpeed(6,false) || (cntBoost==1 && cntMudOil>0)){
            return 2;
        }else if(cntBoost>1 && cntMudOil>0){
            return 3;
        }else{
            return 5;
        }
    }

    private int UseDecelerate(){
        Lane[] curLane=gameState.lanes.get(myCar.position.lane-1);
        boolean flagBoost=false;;
        int startBlock=gameState.lanes.get(0)[0].position.block;
        for(int i=max(myCar.position.block-startBlock+1,0);i<=myCar.position.block-startBlock+getCurSpeed(-1, false);i++){
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            if(curLane[i].terrain==Terrain.BOOST){
                flagBoost=true;
                break;
            }
        }
        if(flagBoost && UseAccelerate()!=5){
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
        for(int i=max(myCar.position.block-startBlock,0);i<=myCar.position.block-startBlock+myCar.speed;i++){
            if(curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.TWEET){
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
        // Check Enemy position
        int curLaneIdx = myCar.position.lane-1;
        boolean canHit = false;
        if(myCar.position.block < opponent.position.block){
            canHit = true;
        }
        for(int i = curLaneIdx-1; i <= curLaneIdx+1; i++){
            canHit = false;
            if(0<=i && i <=3){
                if(i+1 == opponent.position.lane){
                    canHit = true;
                    break;
                }
            }
        }
        // Check is there Power_up
        boolean hasPU = hasPowerUp(PowerUps.EMP);
        // Check is there any obstacle
        Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        int obstacleDMG = 0; 
        for(int i=max(myCar.position.block-startBlock,0);i<=myCar.position.block-startBlock+myCar.speed;i++){
            if(curLane[i].terrain==Terrain.WALL || curLane[i].terrain==Terrain.TWEET){
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
        else if(hasPU && canHit && myCar.speed==maxSpeed && obstacleDMG == 0){
            return 5;
        }
        else{
            return 0;
        }
    }
    
    private int UseLizard(){
        // Check what terrain that will be skipped
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        boolean isWallTruck = blocks.contains((Terrain.WALL));  // Cybertruck apa?
        boolean isMudOil = blocks.contains((Terrain.MUD)) || blocks.contains((Terrain.OIL_SPILL));
        boolean isPowerUPskip = blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.OIL_POWER) || blocks.contains(Terrain.EMP) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.TWEET);
        
        // Check what terrain while landing
        Lane[] curLane = gameState.lanes.get(myCar.position.lane-1);
        int startBlock=gameState.lanes.get(0)[0].position.block;
        Terrain landing = curLane[myCar.position.block-startBlock+myCar.speed].terrain;
        boolean safelandingWallTruck = (landing == Terrain.WALL);   // Cybertruck apa?
        boolean safelandingMudOil = (landing == Terrain.MUD || landing == Terrain.OIL_SPILL);
       
        // Check is there power_up
        boolean hasPU = hasPowerUp(PowerUps.LIZARD);
        if( !(hasPU && (isWallTruck || isMudOil)) ){
            return 0;
        }
        else if((hasPU && (isWallTruck || isMudOil)) && !(safelandingMudOil && safelandingWallTruck)){
            return 1;
        }
        else if((hasPU) && (isMudOil) && (safelandingMudOil && safelandingWallTruck) && isPowerUPskip){
            return 2;
        }
        else if((hasPU) && (isWallTruck) && (safelandingMudOil && safelandingWallTruck) && isPowerUPskip){
            return 3;
        }
        else if((hasPU) && (isMudOil) && (safelandingMudOil && safelandingWallTruck) && !isPowerUPskip){
            return 4;
        }
        else if((hasPU) && (isWallTruck) && (safelandingMudOil && safelandingWallTruck) && !isPowerUPskip){
            return 5;
        }
        else{
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
        for (int i = max(curblock - block, 0); i <= curblock - block + getCurSpeed(0, isBooster); i++) {
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
        for (int i = curblock - block + getCurSpeed(0, isBooster) + 1; i <= curblock - block + getCurSpeed(0, isBooster) + NextSpeed; i++){
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
    private boolean isNabrak_turning(int lane, int curblock, boolean isBooster){
        Lane[] curLane=gameState.lanes.get(lane-1);
        int block = gameState.lanes.get(0)[0].position.block;
        for (int i = max(curblock - block, 0); i <= curblock - block + getCurSpeed(0, isBooster); i++) {
            if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                break;
            }
            else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL || curLane[i].terrain==Terrain.WALL){
                return true;
            }
        }
        return false;
    }

    // Fungsi buat coommand Turn_Left
    private int UseTurn_Left(boolean isBooster, boolean isNextBooster){
        if (myCar.position.lane - 1 >= 0){
            if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                return 5;
            }
            else if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                return 4;
            }
            else if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster)){
                return 1;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                return 3;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                return 2;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrak_turning(myCar.position.lane-1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block, isBooster, isNextBooster)){
                return 1;
            }
            else {
                return 0;
            }
        }
        else{
            return 0;
        }
    }

    //Fungsi buat command Turn_Right
    private int UseTurn_Right(boolean isBooster, boolean isNextBooster){
        if (myCar.position.lane + 1 <= 3){
            if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                return 5;
            }
            else if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                return 4;
            }
            else if (isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster)){
                return 1;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                return 3;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && !isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                return 2;
            }
            else if (!isNabrak_turning(myCar.position.lane, myCar.position.block, isBooster) && isNabrak_turning(myCar.position.lane+1, myCar.position.block, isBooster) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block, isBooster, isNextBooster)){
                return 1;
            }
            else {
                return 0;
            }
        }
        else{
            return 0;
        }
    }

    // Fungsi buat command Fix
    private int UseFix(){
        if (myCar.damage == 3 || (myCar.damage == 2 && hasPowerUp(PowerUps.BOOST))){
            return 5;
        }
        else if (myCar.damage == 2){
            return 4;
        }
        else if (myCar.damage == 1){
            return 3;
        }
        else {
            return 2;
        }
    }

    // Fungsi buat command do nothing
    private int UseDo_Nothing(){
        if (isNabrak_turning(myCar.position.lane, myCar.position.block, myCar.boosting) || hasPowerUp(PowerUps.BOOST) || hasPowerUp(PowerUps.OIL) || hasPowerUp(PowerUps.TWEET) || hasPowerUp(PowerUps.LIZARD) || hasPowerUp(PowerUps.EMP)){
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
