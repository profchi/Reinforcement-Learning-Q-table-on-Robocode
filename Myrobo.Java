
/*

Title : Reinforcement Learning on Robocode

Description: Implements Q learning policy of which a robot learns the behaviour of a enemy over time
             and selects appropriate actions to defeat the enemy.
             
             It initially starts with random moves then gives positive and negative rewards based on events that occur
             and overtime is able to select moves optimally to defeat the enemy

Author : Chinedu Isaiah
*/
import robocode.DeathEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.RobotDeathEvent;

import robocode.ScannedRobotEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;

public class Myrobo extends AdvancedRobot {
	
	static double [] states = new double [58320];
	static ArrayList<Double> outputs = new ArrayList<Double>();
	static int games = 0; 
	static double gamesInSection = 0, wins = 0;
	double distanceToRobot, bearingToRobot,xCordinate,yCordinate, energy; //states
	double motion, turning, moveGun, fire;									//actions
	double previous = 0, present;
	double alpha = 0.1;
	double gamma = 0.9;
	double reward = 0;
	static double exploration = 0;
	static double maxQ;
	static boolean initialised = false; 
	
	
	
	public void run() {
		// calculate and store win rate after 200 games
		if(games % 200 == 0 & games != 0 ) {
			outputs.add(wins/gamesInSection);
			wins = 0;
			gamesInSection = 0;
			
		}
		//set exploration to zero after training
		if(games == 60000 )
			exploration = 0;
		++ games;
		++gamesInSection;
		
		while(true) {
			// Keep turning till robot is scanned
			turnLeft(180);
			
			
		}
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		
			stop();
			getParameters(e);	//Get parameters of robot scanned
			takeAction();
			scan();
		}
	// get robot parameters and convert to position on array
	public void getParameters(ScannedRobotEvent e) {
		distanceToRobot = e.getDistance()/200.01;
		distanceToRobot = (int) distanceToRobot;
		bearingToRobot =  ((e.getBearing() + 180) % 360) / 90;
		bearingToRobot = (int) bearingToRobot;
	}
	
	public void onKeyPressed(KeyEvent e) {
		String location;
		//out.println(e.getKeyCode());
		switch (e.getKeyCode()) {
			case 84:
				location = "C:\\Users\\CHINEDU PC\\eclipse-workspace\\Robocodetest\\src\\outputs\\Tracker\\OffPolicy_E=1_Qvalues.txt";
				states = readFile(location);
				out.println("Qvalues");
				break;
			case 70:
				location = "C:\\Users\\CHINEDU PC\\eclipse-workspace\\Robocodetest\\src\\outputs\\Fire\\Greedy,E=0.0_Qvalues.txt";
				states = readFile(location);
				break;
			case 67:
				location = "C:\\Users\\CHINEDU PC\\eclipse-workspace\\Robocodetest\\src\\outputs\\Corners\\Greedy,E=0.0_Qvalues.txt";
				states = readFile(location);
				break;
		}
		
	}
	

	public double [] readFile(String fileToRead) {
		int count = 0;
		double val;
		double [] states = new double [58320];
		File file = new File(fileToRead);
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
		
	        String st;
	        while((st=br.readLine()) != null){
	        	val = Double.parseDouble(st);
	        	states[count] = val;
	            ++count;
	        }
		} 
		catch (IOException e) {
			
			System.out.println("Error Reading");
		}
		return states;
	}
	
	public void takeAction() {
		// Get myrobot parameters and convert to dimension positions
		xCordinate = (int) getX()/ 200;
		if(xCordinate == 4)
			xCordinate = xCordinate - 1;
		yCordinate = (int) getY() / 200;
		if(yCordinate == 3)
			yCordinate = yCordinate - 1;
		energy = getEnergy();
		if(energy > 65)
			energy = 2;
		else if (energy > 15)
			energy = 1;
		else
			energy = 0;
		int maxposition = 0;
		
		// Equivalent index on Q state space
		present = (distanceToRobot*11664) + (bearingToRobot*2916) + (xCordinate*729) + (yCordinate*243) + (energy*81);
		
		maxQ = states[(int)present];
		
		// Get action with maximum Q value in a state
		for (int i = 0 ; i < 81; ++i) {
			if (states[(int)present + i] > maxQ) {
				maxposition = i;
				maxQ = states[(int)present + i];
			}
		}
		
		//Select a random action in the case of exploration
		if (Math.random() < exploration) {
			double random;
			do {
				random = Math.random() * 81;
				random = (int)random;
			}
			while (random == maxposition);
			maxposition = (int)random;
			
		}
		
		//Convert to the equivalent action dimension
		fire = maxposition%3;
		moveGun = (maxposition % 9)/ 3;
		turning = (maxposition % 27)/ 9;
		motion = maxposition/27;
		present += maxposition;
		updateStates();
		previous = present;
		
		
		// Action for robot to perfrom
		setTurnRight((turning - 1)* 90);
		setAhead((motion - 1) * 100);
		setTurnGunRight((moveGun -1) * 90);
		if (fire > 1)
			setFire(fire + 1);
		execute();
	}
	
	//Give rewards for specific events
		public void onDeath(DeathEvent e) {
		reward = -3;
		updateStates();
		out.println("dead");
	}
	
	
	public void onHitRobot(HitRobotEvent e) {
		out.println("hitrobot"); 
		double temp = getEnergy();
		if (temp < energy)
			reward = -0.2;
		else
			reward = 0.1;
		updateStates();
	}
	public void onHitWall (HitWallEvent e) {
		out.println("hitwall"); 
		reward = -0.2;
		updateStates();
	}
	public void onRobotDeath (RobotDeathEvent e) {
		++ wins;
		reward = 3;
		updateStates();
		out.println("gotcha");
		}
	@Override
	public void onBulletHit(BulletHitEvent e) {
		out.println("hit"); 
		reward = 0.5;
		updateStates();
	}
	public void onBulletMissed(BulletMissedEvent e) { 
		out.println("hitmis"); 
		reward = -0.2;
		updateStates();
	}
	public void onHitByBullet(HitByBulletEvent e ) {
		out.println("hitme");
		reward = -0.5;
		updateStates();
		}
	
	//Apply Q learning 
	public void updateStates() {
		
		states[(int)previous] += alpha*(reward + (gamma*maxQ) - states[(int)previous]);
		out.println(previous + " " + present);
		out.println(states[(int)previous] + " " + states[(int) present]);
		reward = 0;
		
	}
	
  // Optional
	// Save survival rate and Q states at end of Battle
	public void onBattleEnded(BattleEndedEvent evnt)
	{
		
		String location = "C:\\Users\\CHINEDU PC\\eclipse-workspace\\Robocodetest\\src\\outputs\\After\\..txt";
		try {
	        BufferedWriter writer = new BufferedWriter(new FileWriter(location));
	            for (double x: outputs) {
	                writer.write(x + "");
	                writer.newLine();
	            }
	            writer.close();
	        } catch (IOException e) {}
		
		location = "C:\\Users\\CHINEDU PC\\eclipse-workspace\\Robocodetest\\src\\outputs\\After\\Qvalues.txt";
		try {
	        BufferedWriter writer = new BufferedWriter(new FileWriter(location));
	            for (double x: states) {
	                writer.write(x + "");
	                writer.newLine();
	            }
	            writer.close();
	        } catch (IOException e) {}
	}
}
