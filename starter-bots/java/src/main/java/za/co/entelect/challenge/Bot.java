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
        for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
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
        int block=gameState.lanes.get(0)[0].position.block;
        for(int i=max(block-myCar.position.block,0);i<=block-myCar.position.block+getCurSpeed(0, true);i++){
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
        int block=gameState.lanes.get(0)[0].position.block;
        for(int i=max(block-myCar.position.block,0);i<=block-myCar.position.block+getCurSpeed(1, false);i++){
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
        int block=gameState.lanes.get(0)[0].position.block;
        for(int i=max(block-myCar.position.block,0);i<=block-myCar.position.block+getCurSpeed(-1, false);i++){
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
}
