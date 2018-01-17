import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
public class FickleFish extends MCPlayer {
private final static int timePerMoveBase=1150;
private final static int timePerMoveCoefficient=125;
private final static int timePerCollapse=50;
private int timePerMove=timePerMoveBase;
private UCBNode[][]scores=new UCBNode[16][16];
public static void main(String[]args)throws IOException{
new FickleFish();
}
public FickleFish()throws IOException{
System.err.println("R FickleFish");
for(int i=0;i<16;i++){
for(int j=0;j<16;j++){
scores[i][j]=new UCBNode();
}
}
game.run(this);
}
@Override
public byte[]selectFirstMove(){
return new byte[]{5,6};
}
@Override
public byte[]selectMove(){
storeCurrentState();
byte nFree=0;
byte[]legalMoves=new byte[16];
for(byte i=0;i<16;i++){
if(game.board[i]==null){
legalMoves[nFree]=i;
nFree++;
}
}
if(nFree==1){
return new byte[]{legalMoves[0],legalMoves[0]};
}
for(int i=0;i<nFree-1;i++){
for(int j=i+1;j<nFree;j++){
scores[i][j].reset();
}
}
int k;
byte move1;
byte move2;
float maxPriority;
float sqrtlogn;
float priority;
long total=0;
timePerMove-=timePerMoveCoefficient;
long start=System.currentTimeMillis();
while(System.currentTimeMillis()-start<timePerMove){
for(k=0;k<10;k++){
move1=-1;
move2=-1;
maxPriority=0;
sqrtlogn=(float)Math.sqrt(Math.log(total));
for(byte i=0;i<nFree;i++){
for(byte j=(byte)(i+1);j<nFree;j++){
if(scores[i][j].nVisits==0){
priority=Float.POSITIVE_INFINITY;
}else{
priority=scores[i][j].getPriority(sqrtlogn);
}
if(priority>maxPriority){
maxPriority=priority;
move1=i;
move2=j;
}
}
}
game.move(legalMoves[move1],legalMoves[move2]);
playRandomGame();
float score=game.computeMyScore()/(float)30;
total++;
scores[move1][move2].update(score,(float)Math.sqrt(score));
resetState();
}
}
System.err.println("Played a total of "+total+" random games in "+timePerMove+" ms");
int max=0;
byte maxMove1=-1,maxMove2=-1;
for(byte i=0;i<nFree;i++){
for(byte j=(byte)(i+1);j<nFree;j++){
if(scores[i][j].nVisits>max){
max=scores[i][j].nVisits;
maxMove1=i;
maxMove2=j;
}
}
}
System.err.println("Best move:"+Character.toString(game.getCharacter(legalMoves[maxMove1]))+Character.toString(game.getCharacter(legalMoves[maxMove2]))+" with "+max+"("+(100*max/(double)total)+"%)plays and average score "+scores[maxMove1][maxMove2].averageScore);
return new byte[]{legalMoves[maxMove1],legalMoves[maxMove2]};
}
@Override
public byte selectCollapse(byte move1,byte move2){
storeCurrentState();
int score1=-1,score2=-1;
boolean lose1=false,lose2=false;
game.collapse(move1);
if(game.gameOver()||game.currentTurn==17){
score1=game.computeMyScore();
lose1=game.getOutcome()<0;
}
resetBoard();
game.collapse(move2);
if(game.gameOver()||game.currentTurn==17){
score2=game.computeMyScore();
lose2=game.getOutcome()<0;
}
resetBoard();
if(lose1&&!lose2){
System.err.println("If I collapse to "+Character.toString(game.getCharacter(move1))+" I lose,so collapse to "+Character.toString(game.getCharacter(move2)));
return move2;
}else if(lose2&&!lose1){
System.err.println("If I collapse to "+Character.toString(game.getCharacter(move2))+" I lose,so collapse to "+Character.toString(game.getCharacter(move1)));
return move1;
}
if(score1>-1){
if(score2>-1){
System.err.println("Both collapse options end the game,but "+(score1>score2?Character.toString(game.getCharacter(move1)):Character.toString(game.getCharacter(move2)))+" gives me "+(score1>score2?score1:score2)+" points,while "+(score1>score2?Character.toString(game.getCharacter(move2)):Character.toString(game.getCharacter(move1)))+" only gives me "+(score1>score2?score2:score1));
return(score1>score2?move1:move2);
}else{
System.err.println("If I collapse to "+Character.toString(game.getCharacter(move1))+" I win,so do it.");
return move1;
}
}else if(score2>-1){
System.err.println("If I collapse to "+Character.toString(game.getCharacter(move2))+" I win,so do it.");
return move2;
}
long n1=0,n2=0,s1=0,s2=0;
boolean collapse1=true;
long start=System.currentTimeMillis();
while(System.currentTimeMillis()-start<timePerCollapse){
game.collapse(collapse1?move1:move2);
playRandomGame();
int score=game.computeMyScore();
if(collapse1){
s1+=score;
n1++;
}else{
s2+=score;
n2++;
}
collapse1=!collapse1;
resetState();
}
System.err.println("Tried both colapse options roughly "+n1+" times");
if(s1/(double)n1>s2/(double)n2){
System.err.println("Best option:"+Character.toString(game.getCharacter(move1))+" with average score "+(s1/(double)(n1*30)));
return move1;
}else{
System.err.println("Best option:"+Character.toString(game.getCharacter(move2))+" with average score "+(s2/(double)(n2*30)));
return move2;
}
}
class UCBNode extends Node{
@Override
public Pair<Node,Boolean>performNextMove(QTTTGame game){
return null;
}
}
}
class Pair<T1, T2> {
private T1 first;
private T2 second;
public Pair(T1 first,T2 second){
this.first=first;
this.second=second;
}
public T1 getFirst(){
return first;
}
public void setFirst(T1 first){
this.first=first;
}
public T2 getSecond(){
return second;
}
public void setSecond(T2 second){
this.second=second;
}
@Override
public String toString(){
return "("+first+","+second+")";
}
@Override
@SuppressWarnings("unchecked")
public boolean equals(Object obj){
if(obj==null){
return false;
}
if(getClass()!=obj.getClass()){
return false;
}
final Pair<T1,T2>other=(Pair<T1,T2>)obj;
if(this.first!=other.first&&(this.first==null||!this.first.equals(other.first))){
return false;
}
if(this.second!=other.second&&(this.second==null||!this.second.equals(other.second))){
return false;
}
return true;
}
@Override
public int hashCode(){
int hash=7;
hash=41*hash+(this.first!=null?this.first.hashCode():0);
hash=41*hash+(this.second!=null?this.second.hashCode():0);
return hash;
}
}
class QTTTGame {
private static final boolean DEBUG_GAME=true;
public static final XorShiftRandom random=new XorShiftRandom(System.nanoTime());
public State[]board=new State[16];
public int binaryBoard;
public SuperPositionList[]superpositions=new SuperPositionList[16];
public byte currentTurn;
public boolean meFirst;
public SuperPosition[]moves1=new SuperPosition[17];
public SuperPosition[]moves2=new SuperPosition[17];
private SuperPosition[]toCollapse=new SuperPosition[32];
public QTTTGame(){
initialize();
}
public final void initialize(){
currentTurn=1;
meFirst=false;
binaryBoard=0;
for(int i=0;i<16;i++){
board[i]=null;
superpositions[i]=new SuperPositionList();
moves1[i+1]=new SuperPosition((byte)0,(byte)(i+1),meFirst);
moves2[i+1]=new SuperPosition((byte)0,(byte)(i+1),meFirst);
moves1[i+1].twin=moves2[i+1];
moves2[i+1].twin=moves1[i+1];
}
}
public void run(QTTTPlayer player)throws IOException{
BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
String input=in.readLine();
String output="";
while(input!=null&&!input.equals("Quit")){
if(DEBUG_GAME){
System.err.println("Input:"+input+" at turn "+currentTurn);
}
if(input.equals("Start")){
meFirst=true;
for(int i=1;i<17;i++){
moves1[i].switchOwner();
moves2[i].switchOwner();
}
byte[]myMove=player.selectFirstMove();
move(myMove[0],myMove[1]);
player.notifyMove(myMove[0],myMove[1]);
output+=Character.toString(getCharacter(myMove[0]));
output+=Character.toString(getCharacter(myMove[1]));
if(DEBUG_GAME){
System.err.println("Board after move "+Character.toString(getCharacter(myMove[0]))+Character.toString(getCharacter(myMove[1])));
printBoard();
}
}else{
if(input.endsWith("!")){
input=input.substring(0,input.length()-1);
}
if(input.length()==1||input.length()==3){
byte position=getPosition(input.charAt(0));
input=input.substring(1);
collapse(position);
player.notifyCollapse(position);
if(DEBUG_GAME){
System.err.println("Board after collapse of("+(position/4)+","+(position%4)+")");
printBoard();
}
}
if(input.length()>0){
byte move1=getPosition(input.charAt(0));
byte move2=getPosition(input.charAt(1));
boolean collapse=move(move1,move2);
player.notifyMove(move1,move2);
if(DEBUG_GAME){
System.err.println("Board after move "+input);
printBoard();
System.err.println("cycle:"+collapse);
}
if(collapse){
byte collapsePos=player.selectCollapse(move1,move2);
output+=getCharacter(collapsePos);
collapse(collapsePos);
player.notifyCollapse(collapsePos);
if(DEBUG_GAME){
System.err.println("Board after collapse of "+Character.toString(getCharacter(collapsePos)));
printBoard();
}
}
if(currentTurn<17&&!gameOver()){
byte[]myMove=player.selectMove();
move(myMove[0],myMove[1]);
player.notifyMove(myMove[0],myMove[1]);
if(DEBUG_GAME){
System.err.println("Board after move "+Character.toString(getCharacter(myMove[0]))+Character.toString(getCharacter(myMove[1])));
printBoard();
}
output+=Character.toString(getCharacter(myMove[0]));
output+=Character.toString(getCharacter(myMove[1]));
}
}
}
System.out.println(output);
System.out.flush();
System.err.flush();
output="";
input=in.readLine();
}
}
public void printBoard(){
String[]text=new String[16];
for(int i=0;i<16;i++){
if(board[i]==null){
text[i]="[";
for(SuperPosition s:superpositions[i]){
text[i]+=(s.isMine?"M"+s.time:"H"+s.time);
text[i]+=",";
}
text[i]+="]";
text[i]=text[i].replaceAll(",\\]","]");
}else{
text[i]=(board[i].isMine?"M"+board[i].time:"H"+board[i].time);
}
}
System.err.println(String.format("%s|%s|%s|%s",text[0],text[1],text[2],text[3]));
System.err.println(String.format("%s|%s|%s|%s",text[4],text[5],text[6],text[7]));
System.err.println(String.format("%s|%s|%s|%s",text[8],text[9],text[10],text[11]));
System.err.println(String.format("%s|%s|%s|%s",text[12],text[13],text[14],text[15]));
System.err.println();
}
public boolean move(byte pos1,byte pos2){
boolean cycle=(pos1==pos2);
SuperPosition move1=moves1[currentTurn];
SuperPosition move2=moves2[currentTurn];
move1.position=pos1;
move2.position=pos2;
if(superpositions[pos1].isEmpty()){
if(superpositions[pos2].isEmpty()){
move1.parent=null;
move1.rank=1;
move2.parent=move1;
}else{
SuperPosition component=superpositions[pos2].getConnectedComponent();
move1.parent=component;
move2.parent=component;
}
}else{
if(superpositions[pos2].isEmpty()){
SuperPosition component=superpositions[pos1].getConnectedComponent();
move1.parent=component;
move2.parent=component;
}else{
SuperPosition component1=superpositions[pos1].getConnectedComponent();
SuperPosition component2=superpositions[pos2].getConnectedComponent();
if(component1==component2){
cycle=true;
move1.parent=component1;
move2.parent=component1;
}else{
SuperPosition newComponent=SuperPosition.mergeConnectedComponents(component1,component2);
move1.parent=newComponent;
move2.parent=newComponent;
}
}
}
superpositions[pos1].add(move1);
superpositions[pos2].add(move2);
currentTurn++;
return cycle;
}
public void collapse(byte pos){
toCollapse[0]=superpositions[pos].getLast();
int i=0;
int size=1;
while(i<size){
byte p=toCollapse[i].position;
board[p]=toCollapse[i];
if(toCollapse[i].isMine){
binaryBoard|=(0x8000>>>p);
}else{
binaryBoard|=(0x80000000>>>p);
}
for(int j=0;j<superpositions[p].size();j++){
SuperPosition move=superpositions[p].get(j);
if(move!=toCollapse[i]&&board[move.twin.position]==null){
toCollapse[size]=move.twin;
size++;
}
}
i++;
}
}
public boolean gameOver(){
return
(binaryBoard&0xF)==0xF||(binaryBoard&0xF0)==0xF0||(binaryBoard&0xF00)==0xF00||(binaryBoard&0xF000)==0xF000
||(binaryBoard&0xF0000)==0xF0000||(binaryBoard&0xF00000)==0xF00000||(binaryBoard&0xF000000)==0xF000000||(binaryBoard&0xF0000000)==0xF0000000
||(binaryBoard&0x8888)==0x8888||(binaryBoard&0x4444)==0x4444||(binaryBoard&0x2222)==0x2222||(binaryBoard&0x1111)==0x1111
||(binaryBoard&0x88880000)==0x88880000||(binaryBoard&0x44440000)==0x44440000||(binaryBoard&0x22220000)==0x22220000||(binaryBoard&0x11110000)==0x11110000
||(binaryBoard&0x8421)==0x8421||(binaryBoard&0x1248)==0x1248
||(binaryBoard&0x84210000)==0x84210000||(binaryBoard&0x12480000)==0x12480000;
}
public int getOutcome(){
if(gameOver()){
int iWon=iWon();
int heWon=heWon();
if(iWon<heWon){
if(heWon==Integer.MAX_VALUE){
return 2;
}else{
return 1;
}
}else{
if(iWon==Integer.MAX_VALUE){
return-2;
}else{
return-1;
}
}
}else{
return 0;
}
}
public int computeMyScore(){
int myBonus=myBonus();
if(gameOver()){
int iWon=iWon();
int heWon=heWon();
if(iWon<heWon){
if(heWon==Integer.MAX_VALUE){
return 20+myBonus;
}else{
return 15+myBonus;
}
}else{
if(iWon==Integer.MAX_VALUE){
return myBonus;
}else{
return 5+myBonus;
}
}
}else{
return 10+myBonus;
}
}
public int computeHisScore(){
int bonus=hisBonus();
if(gameOver()){
int iWon=iWon();
int heWon=heWon();
if(iWon<heWon){
if(heWon==Integer.MAX_VALUE){
return bonus;
}else{
return 5+bonus;
}
}else{
if(iWon==Integer.MAX_VALUE){
return 20+bonus;
}else{
return 15+bonus;
}
}
}else{
return 10+bonus;
}
}
public int iWon(){
if((binaryBoard&0xF000)==0xF000){
return Math.max(board[0].time,Math.max(board[1].time,Math.max(board[2].time,board[3].time)));
}
if((binaryBoard&0xF00)==0xF00){
return Math.max(board[4].time,Math.max(board[5].time,Math.max(board[6].time,board[7].time)));
}
if((binaryBoard&0xF0)==0xF0){
return Math.max(board[8].time,Math.max(board[9].time,Math.max(board[10].time,board[11].time)));
}
if((binaryBoard&0xF)==0xF){
return Math.max(board[12].time,Math.max(board[13].time,Math.max(board[14].time,board[15].time)));
}
if((binaryBoard&0x8888)==0x8888){
return Math.max(board[0].time,Math.max(board[4].time,Math.max(board[8].time,board[12].time)));
}
if((binaryBoard&0x4444)==0x4444){
return Math.max(board[1].time,Math.max(board[5].time,Math.max(board[9].time,board[13].time)));
}
if((binaryBoard&0x2222)==0x2222){
return Math.max(board[2].time,Math.max(board[6].time,Math.max(board[10].time,board[14].time)));
}
if((binaryBoard&0x1111)==0x1111){
return Math.max(board[3].time,Math.max(board[7].time,Math.max(board[11].time,board[15].time)));
}
if((binaryBoard&0x8421)==0x8421){
return Math.max(board[0].time,Math.max(board[5].time,Math.max(board[10].time,board[15].time)));
}
if((binaryBoard&0x1248)==0x1248){
return Math.max(board[3].time,Math.max(board[6].time,Math.max(board[9].time,board[12].time)));
}
return Integer.MAX_VALUE;
}
public int heWon(){
if((binaryBoard&0xF0000000)==0xF0000000){
return Math.max(board[0].time,Math.max(board[1].time,Math.max(board[2].time,board[3].time)));
}
if((binaryBoard&0xF000000)==0xF000000){
return Math.max(board[4].time,Math.max(board[5].time,Math.max(board[6].time,board[7].time)));
}
if((binaryBoard&0xF00000)==0xF00000){
return Math.max(board[8].time,Math.max(board[9].time,Math.max(board[10].time,board[11].time)));
}
if((binaryBoard&0xF0000)==0xF0000){
return Math.max(board[12].time,Math.max(board[13].time,Math.max(board[14].time,board[15].time)));
}
if((binaryBoard&0x88880000)==0x88880000){
return Math.max(board[0].time,Math.max(board[4].time,Math.max(board[8].time,board[12].time)));
}
if((binaryBoard&0x44440000)==0x44440000){
return Math.max(board[1].time,Math.max(board[5].time,Math.max(board[9].time,board[13].time)));
}
if((binaryBoard&0x22220000)==0x22220000){
return Math.max(board[2].time,Math.max(board[6].time,Math.max(board[10].time,board[14].time)));
}
if((binaryBoard&0x11110000)==0x11110000){
return Math.max(board[3].time,Math.max(board[7].time,Math.max(board[11].time,board[15].time)));
}
if((binaryBoard&0x84210000)==0x84210000){
return Math.max(board[0].time,Math.max(board[5].time,Math.max(board[10].time,board[15].time)));
}
if((binaryBoard&0x12480000)==0x12480000){
return Math.max(board[3].time,Math.max(board[6].time,Math.max(board[9].time,board[12].time)));
}
return Integer.MAX_VALUE;
}
public int myBonus(){
int bonus=0;
if((binaryBoard&0xCC00)==0xCC00){
bonus+=2;
}
if((binaryBoard&0x0CC0)==0x0CC0){
bonus+=2;
}
if((binaryBoard&0x00CC)==0x00CC){
bonus+=2;
}
if((binaryBoard&0x6600)==0x6600){
bonus+=2;
}
if((binaryBoard&0x0660)==0x0660){
bonus+=2;
}
if((binaryBoard&0x0066)==0x0066){
bonus+=2;
}
if((binaryBoard&0x3300)==0x3300){
bonus+=2;
}
if((binaryBoard&0x0330)==0x0330){
bonus+=2;
}
if((binaryBoard&0x0033)==0x0033){
bonus+=2;
}
if((binaryBoard&0xE000)==0xE000){
bonus++;
}
if((binaryBoard&0x0E00)==0x0E00){
bonus++;
}
if((binaryBoard&0x00E0)==0x00E0){
bonus++;
}
if((binaryBoard&0x000E)==0x000E){
bonus++;
}
if((binaryBoard&0x7000)==0x7000){
bonus++;
}
if((binaryBoard&0x0700)==0x0700){
bonus++;
}
if((binaryBoard&0x0070)==0x0070){
bonus++;
}
if((binaryBoard&0x0007)==0x0007){
bonus++;
}
if((binaryBoard&0x8880)==0x8880){
bonus++;
}
if((binaryBoard&0x0888)==0x0888){
bonus++;
}
if((binaryBoard&0x4440)==0x4440){
bonus++;
}
if((binaryBoard&0x0444)==0x0444){
bonus++;
}
if((binaryBoard&0x2220)==0x2220){
bonus++;
}
if((binaryBoard&0x0222)==0x0222){
bonus++;
}
if((binaryBoard&0x1110)==0x1110){
bonus++;
}
if((binaryBoard&0x0111)==0x0111){
bonus++;
}
if((binaryBoard&0x8420)==0x8420){
bonus++;
}
if((binaryBoard&0x0842)==0x0842){
bonus++;
}
if((binaryBoard&0x4210)==0x4210){
bonus++;
}
if((binaryBoard&0x0421)==0x0421){
bonus++;
}
if((binaryBoard&0x1240)==0x1240){
bonus++;
}
if((binaryBoard&0x0124)==0x0124){
bonus++;
}
if((binaryBoard&0x2480)==0x2480){
bonus++;
}
if((binaryBoard&0x0248)==0x0248){
bonus++;
}
return bonus;
}
public int hisBonus(){
int bonus=0;
if((binaryBoard&0xCC000000)==0xCC000000){
bonus+=2;
}
if((binaryBoard&0x0CC00000)==0x0CC00000){
bonus+=2;
}
if((binaryBoard&0x00CC0000)==0x00CC0000){
bonus+=2;
}
if((binaryBoard&0x66000000)==0x66000000){
bonus+=2;
}
if((binaryBoard&0x06600000)==0x06600000){
bonus+=2;
}
if((binaryBoard&0x00660000)==0x00660000){
bonus+=2;
}
if((binaryBoard&0x33000000)==0x33000000){
bonus+=2;
}
if((binaryBoard&0x03300000)==0x03300000){
bonus+=2;
}
if((binaryBoard&0x00330000)==0x00330000){
bonus+=2;
}
if((binaryBoard&0xE0000000)==0xE0000000){
bonus++;
}
if((binaryBoard&0x0E000000)==0x0E000000){
bonus++;
}
if((binaryBoard&0x00E00000)==0x00E00000){
bonus++;
}
if((binaryBoard&0x000E0000)==0x000E0000){
bonus++;
}
if((binaryBoard&0x70000000)==0x70000000){
bonus++;
}
if((binaryBoard&0x07000000)==0x07000000){
bonus++;
}
if((binaryBoard&0x00700000)==0x00700000){
bonus++;
}
if((binaryBoard&0x00070000)==0x00070000){
bonus++;
}
if((binaryBoard&0x88800000)==0x88800000){
bonus++;
}
if((binaryBoard&0x08880000)==0x08880000){
bonus++;
}
if((binaryBoard&0x44400000)==0x44400000){
bonus++;
}
if((binaryBoard&0x04440000)==0x04440000){
bonus++;
}
if((binaryBoard&0x22200000)==0x22200000){
bonus++;
}
if((binaryBoard&0x02220000)==0x02220000){
bonus++;
}
if((binaryBoard&0x11100000)==0x11100000){
bonus++;
}
if((binaryBoard&0x01110000)==0x01110000){
bonus++;
}
if((binaryBoard&0x84200000)==0x84200000){
bonus++;
}
if((binaryBoard&0x08420000)==0x08420000){
bonus++;
}
if((binaryBoard&0x42100000)==0x42100000){
bonus++;
}
if((binaryBoard&0x04210000)==0x04210000){
bonus++;
}
if((binaryBoard&0x12400000)==0x12400000){
bonus++;
}
if((binaryBoard&0x01240000)==0x01240000){
bonus++;
}
if((binaryBoard&0x24800000)==0x24800000){
bonus++;
}
if((binaryBoard&0x02480000)==0x02480000){
bonus++;
}
return bonus;
}
public byte getPosition(char move){
return(byte)(move-'A');
}
public char getCharacter(byte pos){
return(char)('A'+pos);
}
}
abstract class QTTTPlayer {
protected QTTTGame game;
public QTTTPlayer(){
game=new QTTTGame();
}
public abstract byte[]selectFirstMove();
public abstract byte[]selectMove();
public abstract byte selectCollapse(byte move1,byte move2);
public void notifyMove(byte move1,byte move2){
}
public void notifyCollapse(byte collapse){
}
}
class State {
public boolean isMine;
public byte position;
public byte time;
public State(byte position,byte time,boolean meFirst){
this.position=position;
this.time=time;
if(meFirst){
isMine=time%2==1;
}else{
isMine=time%2==0;
}
}
public void switchOwner(){
isMine=!isMine;
}
}
class SuperPosition extends State {
public SuperPosition twin;
public SuperPosition parent;
public byte rank;
public SuperPosition(byte position,byte time,boolean meFirst){
super(position,time,meFirst);
parent=null;
rank=0;
}
public SuperPosition(byte position,byte time,boolean meFirst,SuperPosition parent,byte rank){
super(position,time,meFirst);
this.parent=parent;
this.rank=rank;
}
public SuperPosition getConnectedComponent(){
if(parent==null){
return this;
}else{
parent=parent.getConnectedComponent();
return parent;
}
}
public static SuperPosition mergeConnectedComponents(SuperPosition a,SuperPosition b){
if(a.rank<b.rank){
a.parent=b;
return b;
}else if(b.rank<a.rank){
b.parent=a;
return a;
}else{
b.parent=a;
a.rank++;
return a;
}
}
}
class SuperPositionList extends ArrayList<SuperPosition> {
public SuperPositionList(){
}
public SuperPositionList(Collection<?extends SuperPosition>c){
super(c);
}
public SuperPosition getFirst(){
if(isEmpty()){
return null;
}else{
return get(0);
}
}
public SuperPosition getLast(){
if(isEmpty()){
return null;
}else{
return get(size()-1);
}
}
public SuperPosition getConnectedComponent(){
if(isEmpty()){
return null;
}else{
return get(0).getConnectedComponent();
}
}
}
class XorShiftRandom {
private static final long DEFAULT_SEED=88172645463325252L;
private static long x;
public XorShiftRandom(){
this(DEFAULT_SEED);
}
public XorShiftRandom(long seed){
if(seed!=0){
x=seed;
}else{
x=DEFAULT_SEED;
}
}
public long randomLong(){
x^=(x<<21);
x^=(x>>>35);
x^=(x<<4);
return x;
}
public byte next16(){
return(byte)(randomLong()&0xF);
}
public boolean nextBool(){
return(randomLong()&0x1)!=0;
}
}
abstract class MCPlayer extends QTTTPlayer {
private State[]realBoard=new State[16];
private int realBinaryBoard;
private byte realCurrentTurn;
private SuperPosition[]realComponents=new SuperPosition[16];
private State[]realBoard2=new State[16];
private int realBinaryBoard2;
public void playRandomGame(){
while(game.currentTurn<17&&!game.gameOver()){
byte move1=randomMove();
byte move2=randomMove();
while(move1==move2&&game.currentTurn<16){
move2=randomMove();
}
if(game.move(move1,move2)){
if(QTTTGame.random.nextBool()){
game.collapse(move1);
}else{
game.collapse(move2);
}
}
}
}
private void selectSimulationCollapse(byte move1,byte move2){
boolean myDecision=(game.meFirst?game.currentTurn%2==1:game.currentTurn%2==0);
storeCurrentBoard2();
game.collapse(move1);
if(game.gameOver()){
int outcome=game.getOutcome();
if(outcome>0){
if(myDecision){
}else{
resetBoard2();
game.collapse(move2);
}
return;
}else if(outcome<0){
if(myDecision){
resetBoard2();
game.collapse(move2);
}else{
}
return;
}
}
resetBoard2();
game.collapse(move2);
if(game.gameOver()){
int outcome=game.getOutcome();
if(outcome>0){
if(myDecision){
}else{
resetBoard2();
game.collapse(move1);
}
return;
}else if(outcome<0){
if(myDecision){
resetBoard2();
game.collapse(move1);
}else{
}
return;
}
}
if(QTTTGame.random.nextBool()){
game.collapse(move1);
}else{
game.collapse(move2);
}
}
public void storeCurrentState(){
System.arraycopy(game.board,0,realBoard,0,16);
realBinaryBoard=game.binaryBoard;
realCurrentTurn=game.currentTurn;
for(int i=0;i<16;i++){
realComponents[i]=game.superpositions[i].getConnectedComponent();
}
}
public void storeCurrentBoard(){
System.arraycopy(game.board,0,realBoard,0,16);
realBinaryBoard=game.binaryBoard;
}
public void storeCurrentBoard2(){
System.arraycopy(game.board,0,realBoard2,0,16);
realBinaryBoard2=game.binaryBoard;
}
public void resetState(){
System.arraycopy(realBoard,0,game.board,0,16);
game.binaryBoard=realBinaryBoard;
game.currentTurn=realCurrentTurn;
for(int i=0;i<16;i++){
game.superpositions[i].clear();
}
for(int i=1;i<game.currentTurn;i++){
byte pos1=game.moves1[i].position;
SuperPosition comp1=realComponents[pos1];
if(comp1!=null){
game.superpositions[game.moves1[i].position].add(game.moves1[i]);
if(comp1==game.moves1[i]){
game.moves1[i].parent=null;
}else{
game.moves1[i].parent=comp1;
}
}else{
System.out.println("Component is null!");
}
byte pos2=game.moves2[i].position;
SuperPosition comp2=realComponents[pos2];
if(comp2!=null){
game.superpositions[game.moves2[i].position].add(game.moves2[i]);
if(comp2==game.moves2[i]){
game.moves2[i].parent=null;
}else{
game.moves2[i].parent=comp2;
}
}else{
System.out.println("Component is null!");
}
}
}
public void resetBoard(){
System.arraycopy(realBoard,0,game.board,0,16);
game.binaryBoard=realBinaryBoard;
}
public void resetBoard2(){
System.arraycopy(realBoard2,0,game.board,0,16);
game.binaryBoard=realBinaryBoard2;
}
public byte randomMove(){
byte pos=QTTTGame.random.next16();
while(game.board[pos]!=null){
pos=QTTTGame.random.next16();
}
return pos;
}
}
abstract class Node {
private static final float SQRT2=(float)Math.sqrt(2);
protected int nVisits;
private float sqrtVisits;
float averageScore;
protected float variance;
public abstract Pair<Node,Boolean>performNextMove(QTTTGame game);
public float getPriority(float sqrtLognVisitsParent){
float sqrtLognDivS=sqrtLognVisitsParent/sqrtVisits;
float varEstimation=variance+SQRT2*sqrtLognDivS;
if(varEstimation<0.25){
return averageScore+sqrtLognDivS*(float)Math.sqrt(varEstimation);
}else{
return averageScore+sqrtLognDivS*0.5f;
}
}
public void update(float score01,float score01Sq){
float sumScores=averageScore*nVisits+score01;
float sumSquaredScores=(variance+averageScore*averageScore)*nVisits+score01Sq;
nVisits++;
sqrtVisits=(float)Math.sqrt(nVisits);
averageScore=sumScores/nVisits;
variance=(sumSquaredScores/nVisits)-averageScore*averageScore;
}
public void reset(){
nVisits=0;
sqrtVisits=0;
averageScore=0;
variance=0;
}
@Override
public String toString(){
return "Node{"+"nVisits="+nVisits+"sqrtVisits="+sqrtVisits+"averageScore="+averageScore+"variance="+variance+'}';
}
}
