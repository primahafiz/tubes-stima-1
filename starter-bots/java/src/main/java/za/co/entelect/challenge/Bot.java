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
        com[0] = 0;
        com[1] = 0;     // Turn Right
        com[2] = 0;     // Turn Left
        com[3] = UseLizard();
        com[4] = UseBoost();
        com[5] = UseAccelerate();
        com[6] = UseEMP();
        com[7] = 0;     // Use Tweet
        com[8] = UseOil();
        com[9] = UseDecelerate();
        com[10] = 0;
        int idxMax = -1;
        int max = -1;
        for(int i = 0; i < 11; i++){
            if(com[i] > max){
                max = com[i];
                idxMax = i;
            }
        }
        if(max == 0){
            idxMax = 10;
        }


        // Fungsi seleksi
        if(idxMax == 0){
            return FIX;
        }
        else if(idxMax == 1){
            return TURN_RIGHT;
        }
        else if(idxMax == 2){
            return TURN_LEFT;
        }
        else if(idxMax == 3){
            return LIZARD;
        }
        else if(idxMax == 4){
            return BOOST;
        }
        else if(idxMax == 5){
            return ACCELERATE;
        }
        else if(idxMax == 6){
            return EMP;
        }
        else if(idxMax == 7){
            return NOTHING;     // TWEET
        }
        else if(idxMax == 8){
            return OIL;
        }
        else if(idxMax == 9){
            return DECELERATE;
        }
        else if(idxMax == 10){
            return NOTHING;
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
    
    

    // Fungsi buat ngecek apakah ada obstacle di depannya posisi  
    private boolean isNabrakObstacleInfront_atCurrentLane(int lane, int curblock){
        Lane[] curLane=gameState.lanes.get(lane-1);
        int block=gameState.lanes.get(0)[0].position.block;
        if (!hasPowerUp(PowerUps.BOOST)){
            for (int i = max(block-curblock+1,0); i <= block - curblock + getCurSpeed(0, false); i++) {
                if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                    break;
                }
                else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL || curLane[i].terrain==Terrain.WALL){
                    return true;
                }
            }
        }
        else{
            for (int i = curblock; i <= curblock + getCurSpeed(0, true); i++) {
                if (curLane[i] == null || curLane[i].terrain == Terrain.FINISH) {
                    break;
                }
                else if (curLane[i].terrain==Terrain.MUD || curLane[i].terrain==Terrain.OIL_SPILL || curLane[i].terrain==Terrain.WALL){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNabrak_turnLeft(int lane, int curblock){
        Lane[] curLane=gameState.lanes.get(lane-1);
        int block=gameState.lanes.get(0)[0].position.block;
        if (curLane[curblock].terrain==Terrain.MUD || curLane[curblock].terrain==Terrain.OIL_SPILL || curLane[curblock].terrain==Terrain.WALL){
            return true;
        }
        else{
            return false;
        }
    }

    private boolean isNabrak_turnRight(int lane, int curblock){
        Lane[] curLane=gameState.lanes.get(lane);
        int block=gameState.lanes.get(0)[0].position.block;
        if (curLane[curblock].terrain==Terrain.MUD || curLane[curblock].terrain==Terrain.OIL_SPILL || curLane[curblock].terrain==Terrain.WALL){
            return true;
        }
        else{
            return false;
        }
    }

    // // Fungsi buat coommand Turn_Left
    // private int UseTurn_Left(){
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block) && !isNabrak_turnLeft(myCar.position.lane, myCar.position.block)){
    //         return 5;
    //     }
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block) && !isNabrak_turnLeft(myCar.position.lane, myCar.position.block)){
    //         return 4;
    //     }
    //     if (!isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block)){
    //         return 2;
    //     }
    //     if (!isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane-1, myCar.position.block)){
    //         return 0;
    //     }
    // }

    // //Fungsi buat command Turn_Right
    // private int UseTurn_Right(){
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block) && !isNabrak_turnRight(myCar.position.lane, myCar.position.block)){
    //         return 5;
    //     }
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block) && !isNabrak_turnRight(myCar.position.lane, myCar.position.block)){
    //         return 4;
    //     }
    //     if (!isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && !isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block)){
    //         return 2;
    //     }
    //     if (!isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) && isNabrakObstacleInfront_atCurrentLane(myCar.position.lane+1, myCar.position.block)){
    //         return 0;
    //     }
    // }

    // // Fungsi buat command Fix
    // private int UseFix(){
    //     if (myCar.damage == 3 || (myCar.damage == 2 && hasPowerUp(PowerUps.BOOST))){
    //         return 5;
    //     }
    //     if (myCar.damage == 2){
    //         return 4;
    //     }
    //     if (myCar.damage == 1){
    //         return 3;
    //     }
    //     if (myCar.damage == 0){
    //         return 2;
    //     }
    // }

    // // Fungsi buat command do nothing
    // private int UseDo_Nothing(){
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block)){
    //         return 2;
    //     }
    //     if (!hasPowerUp(PowerUps.BOOST) && !hasPowerUp(PowerUps.OIL) && !hasPowerUp(PowerUps.TWEET) && !hasPowerUp(PowerUps.LIZARD) && !hasPowerUp(PowerUps.EMP)){
    //         return 1;
    //     }
    //     if (isNabrakObstacleInfront_atCurrentLane(myCar.position.lane, myCar.position.block) || hasPowerUp(PowerUps.BOOST) || hasPowerUp(PowerUps.OIL) || hasPowerUp(PowerUps.TWEET) || hasPowerUp(PowerUps.LIZARD) || hasPowerUp(PowerUps.EMP)){
    //         return 0;
    //     }
    // }
}
