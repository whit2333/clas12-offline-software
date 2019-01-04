package org.jlab.rec.rtpc.hit;

import java.awt.Component;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.JFrame;
import org.jlab.groot.data.*;
import org.jlab.groot.fitter.*;
import org.jlab.groot.graphics.*;
import org.jlab.groot.math.F1D;
import org.jlab.groot.ui.TCanvas;


public class TrackFinder {
	
	public void FindTrack(HitParameters params){
		boolean draw = true;
		HashMap<Integer, double[]> ADCMap = params.get_R_adc();
		//HashMap<Integer, HashMap<Integer, int[]>> TIDMap = new HashMap<Integer, HashMap<Integer, int[]>>();
		HashMap<Integer, HashMap<Integer,Vector<Integer>>> TIDMap = new HashMap<Integer, HashMap<Integer,Vector<Integer>>>();
		//HashMap<Integer, double[]> PadThresh = new HashMap<Integer, double[]>();
		Vector<Integer> PadNum = params.get_PadNum();
		Vector<Integer> TIDVec = new Vector<>();
		TIDVec.add(1);
		int Pad = 0;
		int TrigWindSize = params.get_TrigWindSize();
		//int StepSize = params.get_StepSize();
		int StepSize = 120;
		double thresh = 1e-5;
		double ADC = 0;
		int maxconcpads = 0;
		int concpads = 0;
		int maxconctime = 0;
		double checkpadphi = 0;
		double checkpadphiprev = 0;
		double checkpadz = 0;
		double checkpadzprev = 0;
		double PadPhi = 0;
		double PadZ = 0;
		int TID = 0;
		boolean breakTIDloop = false;
		boolean breakpadindexloop = false;
		int padindexmax = 0;
		double a = 0;
		double b = 0;
		int adjthresh = 1;
		int timeadj = 1;
		int event = params.get_eventnum();
		double time1 = System.currentTimeMillis();
		
		//set variables for timing cut
		int tmin = 0; //min timing in ns
		int trange = 9000; //added to min to calculate max in ns
		int tmax = tmin+trange;
		
		double PAD_W = 2.79; // in mm
		double PAD_S = 80.0; //in mm
		double Num_of_Rows = (2.0*(Math.PI)*PAD_S)/PAD_W;
		
		double PadPhistore = 0;
		double PadZstore = 0;
		boolean PadPhiChanged = false;
		
		//g.setTitleX("Phi");
		//g.setTitleY("Z");
		//g.setMarkerSize(5);
		
		EmbeddedCanvas c = new EmbeddedCanvas();
		JFrame j = new JFrame();
		j.setSize(800,600);
		int test = 1; 
		try {
			FileWriter write2 = new FileWriter("/Users/davidpayette/Documents/FileOutput/Output" + event + ".txt",true);
		
		Collections.sort(PadNum);

		//loop over all times
		TIMELOOP:
		for(int t = 0; t < TrigWindSize; t += StepSize)
		{
			System.out.println("Time is " + t);
			concpads = 0;
			//loop over all pads for each time slice
			PADLOOP:
			for(int p = 0; p < PadNum.size(); p++)
			{
				Pad = PadNum.get(p);
				
				//System.out.println(Pad + " " + t);
				ADC = ADCMap.get(Pad)[t];
				//only pads which have an ADC value above threshold will be assigned a TID
				if(ADC > thresh)
				{	
					//System.out.println("Pad to be checked " + Pad + " checked at time " + t);
					//returns the row and column of the current Pad
					PadPhi = PadPhi(Pad);
					PadZ = PadZ(Pad);
					//System.out.println("Pad " + Pad + " has row and column values " + PadPhi + " " + PadZ);
					//g.addPoint(PadPhi, PadZ, 0, 0);
					
					//loop through all TID's in a vector which will grow to include all future TID's
					TIDLOOP:
					for(int i = 0; i < TIDVec.size(); i++)
					{
						breakTIDloop = false;
						TID = TIDVec.get(i);
						//System.out.println("Current TID " + TID);
						
						//if TID is already in the map
						if(TIDMap.containsKey(TID))
						{
							//loop through all pads in TIDMap and compare there row and column to current Pad
							//System.out.println("TID " + TID + " is already in the map");
							//for(int padindex = 0; padindex < padindexmax; padindex++)
							PADINDEXLOOP:
							for(int padindex = 0; padindex < 100; padindex++)
							{
								//System.out.println("Pad index is " + padindex);
								if(padindex < TIDMap.get(TID).get(t).size())
								{
									checkpadphi = PadPhi(TIDMap.get(TID).get(t).get(padindex));								
									checkpadz = 	PadZ(TIDMap.get(TID).get(t).get(padindex));
									//System.out.println("At TID " + TID + " time " + t + " padindex " + padindex + " the check pad's location is " + checkpadphi + " " + checkpadz);
									//System.out.println("Current time " + checkpadphi + " " + PadPhi + " " + checkpadz + " " + PadZ + " " + Math.abs(checkpadphi-PadPhi) + " " + Math.abs(checkpadz - PadZ));
									if(Math.abs(checkpadphi - PadPhi) >= (Num_of_Rows - adjthresh))
									{
										if(checkpadphi > PadPhi)
										{
											checkpadphi -= Num_of_Rows;
											//checkpadz += (checkpadphi-(checkpadphi%4))/4;
										}
										else
										{
											PadPhiChanged = true;
											PadPhistore = PadPhi;
											PadZstore = PadZ;
											PadPhi -= Num_of_Rows;
											//PadZ += (PadPhi-(PadPhi%4))/4;
										}
									}
									//System.out.println("Current time slice " + PadPhi + " " + PadZ + " " + " " + checkpadphi + " " + checkpadz);
									//System.out.println("The comparison values for the pad to be sorted at current time are " + Math.abs(checkpadphi-PadPhi) + " " + Math.abs(checkpadz - PadZ));
									//Check current time slice for adjacency
									if((Math.abs(checkpadphi-PadPhi) <= adjthresh) && (Math.abs(checkpadz - PadZ) <= adjthresh))
									{
										//System.out.println("Found 2");
										//System.out.println("Sorted Current");
										TIDMap.get(TID).get(t).add(Pad);
										//breakTIDloop = true;
										if(PadPhiChanged) 
										{
											PadPhiChanged = false;
											PadPhi = PadPhistore;
											//PadZ = PadZstore;
										}
										break TIDLOOP;
									}
									else {System.out.println("Failed sorting test current");}
									if(PadPhiChanged) 
									{
										PadPhiChanged = false;
										PadPhi = PadPhistore;
										//PadZ = PadZstore;
									}
								}
								
								//System.out.println("new " + padindex);
								//Check previous time slice(s) for adjacency
								if(t>0)
								{
									PREVTIMELOOP:
									for(int prevtime = t - StepSize; (prevtime >= (t - (timeadj*StepSize))) && (prevtime >= 0); prevtime -= StepSize)
									{
										//if(padindex < TIDMap.get(TID).get(t-StepSize).size())
										if(padindex < TIDMap.get(TID).get(prevtime).size())
										{
											//checkpadphiprev = PadPhi(TIDMap.get(TID).get(t-StepSize).get(padindex));
											//checkpadzprev = 	PadZ(TIDMap.get(TID).get(t-StepSize).get(padindex));
											checkpadphiprev = PadPhi(TIDMap.get(TID).get(prevtime).get(padindex));
											checkpadzprev = 	PadZ(TIDMap.get(TID).get(prevtime).get(padindex));
											
											if(Math.abs(checkpadphiprev - PadPhi) >= (Num_of_Rows - adjthresh))
											{
												if(checkpadphiprev > PadPhi)
												{
													checkpadphiprev -= Num_of_Rows;
													//checkpadzprev += (checkpadphiprev-(checkpadphiprev%4))/4;
												}
												else
												{
													PadPhiChanged = true;
													PadPhistore = PadPhi;
													PadZstore = PadZ;
													PadPhi -= Num_of_Rows;
													//PadZ += (PadPhi-(PadPhi%4))/4;
													
												}
											}
											//System.out.println("Previous time slice " + PadPhi + " " + PadZ + " " + " " + checkpadphiprev + " " + checkpadzprev);
											//System.out.println("The comparison values for the pad to be sorted at previous time are " + Math.abs(checkpadphiprev-PadPhi) + " " + Math.abs(checkpadzprev - PadZ));
											if((Math.abs(checkpadphiprev-PadPhi) <= adjthresh && Math.abs(checkpadzprev - PadZ) <= adjthresh))
											{
												//System.out.println("Found 3");
												//System.out.println("Sorted prev");
												TIDMap.get(TID).get(t).add(Pad);
												//breakTIDloop = true;
												//breakpadindexloop = true;
												if(PadPhiChanged) 
												{
													PadPhiChanged = false;
													PadPhi = PadPhistore;
													//PadZ = PadZstore;
												}
												break TIDLOOP;
											}
											else {System.out.println("Failed sorting test previous");}
											if(PadPhiChanged) 
											{
												PadPhiChanged = false;
												PadPhi = PadPhistore;
												//PadZ = PadZstore;
											}
										}
									} //End PREVTIMELOOP
								}
								/*if(breakpadindexloop == true) 
								{
									breakpadindexloop = false;
									break;
								}*/
							} //End PADINDEXLOOP						
						}
						//TID not already in map
						else
						{
							//System.out.println("TID not currently in map " + TID + " " + t + " " + Pad);
							TIDMap.put(TID, new HashMap<Integer, Vector<Integer>>());
							for(int time = 0; time < TrigWindSize; time += StepSize)
							{
								TIDMap.get(TID).put(time, new Vector<>());//add TID to map
							}
							TIDMap.get(TID).get(t).add(Pad);
							TIDVec.add(TID+1);
							break TIDLOOP;
						}
					} //End TIDLOOP
				}
				//else {System.out.println("Pad " + Pad + " failed ADC threshold at time " + t);}
			} //End PADLOOP
			
		} //End TIMELOOP
		
		// PLOTTING //
		HashMap<Integer,GraphErrors> graphmap = new HashMap<Integer,GraphErrors>();
		for(int testTID = 1; testTID <= TIDMap.size(); testTID++)
		{
			for(int a1 = 0; a1 < TrigWindSize; a1+=StepSize)
			{
				for(int a2 = 0; a2 < TIDMap.get(testTID).get(a1).size(); a2++)
				{	
					//g[testTID].setMarkerColor(testTID);
					a = PadPhiRecal(TIDMap.get(testTID).get(a1).get(a2));
					b = PadZRecal(TIDMap.get(testTID).get(a1).get(a2));
					
					//System.out.println(a + " " + b);
					if(!graphmap.containsKey(testTID))
					{
						graphmap.put(testTID, new GraphErrors());
					}
					graphmap.get(testTID).addPoint(a, b, 0, 0);
					write2.write(testTID + "\t" + a1 + "\t" + a + "\t" + b + "\n");
				}
			}
		}
		
		// TIME CUT //
		int tlargest = 0;
		//System.out.println("before " + TIDMap.size());
		int loopsize = TIDMap.size();
		for(int testTID = 1; testTID <= loopsize; testTID++)
		{
			for(int t = 0; t < TrigWindSize; t += StepSize)
			{
				for(int pad = 0; pad < TIDMap.get(testTID).get(t).size(); pad++)
				{
					if(t > tlargest)
					{
						tlargest = t;
					}
					if(t>4000)
					{
						//System.out.println(pad + " " + t);
					}
				}
			}
			//System.out.println(tlargest);
			if(tlargest < tmin || tlargest > tmax)
			{
				TIDMap.remove(testTID);
				//graphmap.remove(testTID);
				
			}
			tlargest = 0;
		}
		
		// CANVAS SETTINGS //
		//System.out.println(tlargest);
		int color = 1;
		int style = 1;
		System.out.println("There are " + TIDMap.size() + " tracks sorted in this event!");
		//System.out.println("after " + TIDMap.size());
		//System.out.println(maxconcpads + " " + maxconctime + " " + TIDVec.size());
		if(draw == true)
		{
			for(int i : TIDMap.keySet())
			{
				//if(i == 1)
				//{
				graphmap.get(i).setMarkerColor(1);
				graphmap.get(i).setMarkerSize(3);
				//graphmap.get(i).addPoint(0, 0, 0, 0);
				//graphmap.get(i).addPoint(180, 100, 0, 0);
				
				//graphmap.get(i).setMarkerStyle(style);
				
				c.draw(graphmap.get(i),"same");
				color++;
				style++;
				
				//}
				//System.out.println("key " + i + " " + TIDMap.size());
			}
			j.setTitle("Track Finder Output");
			j.add(c);
			j.setVisible(true);
		}
		write2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		params.set_TIDMap(TIDMap);
		//TIDMap.clear();
		//System.out.println(System.currentTimeMillis()-time1);
	}
	
	private double PadPhi(int cellID) {
		
		double PAD_W = 2.79; // in mm
		double PAD_S = 80.0; //in mm
        double PAD_L = 4.0; // in mm
	    double RTPC_L=400.0; // in mm
	    
	    double phi_pad = 0;
		    
	    double Num_of_Rows = (2.0*(Math.PI)*PAD_S)/PAD_W;
        double Num_of_Cols = RTPC_L/PAD_L;
	    double TotChan = Num_of_Rows*Num_of_Cols;
		    
	    double PI=Math.PI;
		    
	    

	    double phi_per_pad = PAD_W/PAD_S; // in rad
		
		double chan = (double)cellID;
		double col = chan%Num_of_Cols;
		double row=(chan-col)/Num_of_Cols;
        
        
          //double z_shift = 0.;
   
        phi_pad=(row*phi_per_pad)+(phi_per_pad/2.0);
        if(phi_pad>= 2.0*PI) {
        	phi_pad -= 2.0*PI;
        }
        if(phi_pad<0) 
        	{
        	phi_pad += 2.0*PI;
        	}
   
       return row;
		
	}
	
	private double PadZ(int cellID)
	{
	
		double RTPC_L=400.0; // in mm
		double PAD_L = 4.0; // in mm
		double Num_of_Cols = RTPC_L/PAD_L;
		double z0 = -(RTPC_L/2.0); // front of RTPC in mm at the center of the pad
		double chan = (double)cellID;
		double col = chan%Num_of_Cols;
        double row=(chan-col)/Num_of_Cols;
        double z_shift = row%4;
        double z_pad = 0;
        double testnum = (row-z_shift)/4;
        
        z_pad=z0+(col*PAD_L)+(PAD_L/2.0)+z_shift;
        //if(z_shift == 0 && row > 0) {return col-1;}
        //else{return col;}
        return col;//-testnum;
	}
	private double PadPhiRecal(int cellID) {
		
		double PAD_W = 2.79; // in mm
		double PAD_S = 80.0; //in mm
        double PAD_L = 4.0; // in mm
	    double RTPC_L=400.0; // in mm
	    
	    double phi_pad = 0;
		    
	    double Num_of_Rows = (2.0*(Math.PI)*PAD_S)/PAD_W;
        double Num_of_Cols = RTPC_L/PAD_L;
	    double TotChan = Num_of_Rows*Num_of_Cols;
		    
	    double PI=Math.PI;
		    
	    

	    double phi_per_pad = PAD_W/PAD_S; // in rad
		
		double chan = (double)cellID;
		double col = chan%Num_of_Cols;
		double row=(chan-col)/Num_of_Cols;
        
        
          //double z_shift = 0.;
   
        phi_pad=(row*phi_per_pad)+(phi_per_pad/2.0);
        if(phi_pad>= 2.0*PI) {
        	phi_pad -= 2.0*PI;
        }
        if(phi_pad<0) 
        	{
        	phi_pad += 2.0*PI;
        	}
   
       return row;
		
	}
	
	private double PadZRecal(int cellID)
	{
	
		double RTPC_L=400.0; // in mm
		double PAD_L = 4.0; // in mm
		double Num_of_Cols = RTPC_L/PAD_L;
		double z0 = -(RTPC_L/2.0); // front of RTPC in mm at the center of the pad
		double chan = (double)cellID;
		double col = chan%Num_of_Cols;
        double row=(chan-col)/Num_of_Cols;
        double z_shift = row%4;
        double z_pad = 0;
        //double testnum = (row-z_shift)/4;
        
        z_pad=z0+(col*PAD_L)+(PAD_L/2.0)+z_shift;
        //if(z_shift == 0 && row > 0) {return col-1;}
        //else{return col;}
        return col;//-1*testnum;
	}

}
