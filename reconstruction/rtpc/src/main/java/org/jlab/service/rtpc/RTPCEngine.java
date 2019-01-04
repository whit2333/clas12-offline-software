package org.jlab.service.rtpc;

import java.io.FileNotFoundException;


import java.util.ArrayList;
import java.util.List;

import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.coda.hipo.HipoException;
import org.jlab.coda.jevio.EvioException;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.io.hipo.HipoDataSync;
import org.jlab.rec.rtpc.banks.HitReader;
import org.jlab.rec.rtpc.banks.RecoBankWriter;
import org.jlab.rec.rtpc.banks.RecoBankWriter2;
import org.jlab.rec.rtpc.hit.Hit;
import org.jlab.rec.rtpc.hit.HitDistance;
import org.jlab.rec.rtpc.hit.HitParameters;
import org.jlab.rec.rtpc.hit.HitReconstruction;
import org.jlab.rec.rtpc.hit.MapCombine;
import org.jlab.rec.rtpc.hit.PadAve;
import org.jlab.rec.rtpc.hit.PadFit;
import org.jlab.rec.rtpc.hit.PadHit;
import org.jlab.rec.rtpc.hit.TimeAverage;
import org.jlab.rec.rtpc.hit.TrackFinder;
import org.jlab.rec.rtpc.hit.TrackFinder2;
import org.jlab.rec.rtpc.hit.TrackHitReco;
import org.jlab.rec.rtpc.hit.TrackHitReco2;




public class RTPCEngine extends ReconstructionEngine{

	public int test = 0;
	public int swtch = 2;
	public HitParameters params = new HitParameters();
	
	public RTPCEngine() {
		super("RTPC","charlesg","3.0");
	}

	@Override
	public boolean init() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean processDataEvent(DataEvent event) {
		HitReader hitRead = new HitReader();
		hitRead.fetch_RTPCHits(event);
                
                
		List<Hit> hits = new ArrayList<Hit>();
		//I) get the hits
		hits = hitRead.get_RTPCHits();
		
		//II) process the hits
		//1) exit if hit list is empty
		if(hits.isEmpty()) {
			return true;
		}
		if(event.hasBank("RTPC::pos") && event.hasBank("RTPC::adc"))// && test == 0)
		{
			//test = 0;
			//System.out.println(test);
			PadHit phit = new PadHit();
			phit.bonus_shaping(hits,params);
			PadFit pfit = new PadFit();
			pfit.Fitting(params);
			//TrackFinder TF = new TrackFinder();
			//TF.FindTrack(params);
			if(swtch == 1)
			{
				HitDistance HD = new HitDistance();
				HD.FindDistance(params);
			}
			else 
			{
				TrackFinder2 TF = new TrackFinder2();
				TF.FindTrack2(params,false);
				TimeAverage TA = new TimeAverage();
				TA.TA(params,false);
				MapCombine MC = new MapCombine();
				MC.MC(params,false);
				/*TrackHitReco TR = new TrackHitReco();
				TR.Reco(hits,params);*/
				TrackHitReco2 TR = new TrackHitReco2();
				TR.Reco(hits,params,false);
				RecoBankWriter2 writer = new RecoBankWriter2();				
				DataBank recoBank = writer.fillRTPCHitsBank(event, params);
				event.appendBanks(recoBank);
				//recoBank.show();
			}
			

			
			//PadAve pave = new PadAve();
			//pave.TimeAverage(params);
			

			//HitReconstruction reco = new HitReconstruction();	
			//reco.Reco(params);
			test++;
			
		}
		else
		{
			test++;
			return true;
		}

		/*
		for(Hit h : hits) {
			System.out.println("Hit  "+h.get_Id()+" CellID "+h.get_cellID()+" ADC "+h.get_ADC()+" true Edep "+h.get_EdepTrue()+" Edep "+h.get_Edep()+" Time "+h.get_Time()+" "+
		" true X "+h.get_PosXTrue()+" X "+h.get_PosX()+" true Y "+h.get_PosYTrue()+" Y "+h.get_PosY()+" true Z "+h.get_PosZTrue()+" Z "+h.get_PosZ());
		}*/
		
		
		
		
		return true;
	}
	
	public static void main(String[] args){
		double starttime = System.nanoTime();
		//String inputFile = "/Users/davidpayette/Desktop/5c.2.3/clara/installation/plugins/clas12/2_72_516.hipo";
		//String inputFile = "/Users/davidpayette/Desktop/5c.2.3/clara/installation/plugins/clas12/1000_1_711.hipo";
		//String inputFile = "/Users/davidpayette/Desktop/5c.2.3/clara/installation/plugins/clas12/100_20_731.hipo";
		//String inputFile = "/Users/davidpayette/Desktop/5c.3.5/clara/installation/plugins/clas12/test.hipo";
		String inputFile = "/Users/davidpayette/Desktop/Distribution/clas12-offline-software/1212again.hipo";
		//String inputFile = "/Users/davidpayette/Desktop/5c.2.3/clara/installation/plugins/clas12/100_20_802.hipo";
		//String inputFile = "/Users/davidpayette/Desktop/5c.2.3/clara/installation/plugins/clas12/10p.hipo";
		//String inputFile = args[0];
		String outputFile = "/Users/davidpayette/Desktop/5b.7.4/myClara/tout_working.hipo";
		
		System.err.println(" \n[PROCESSING FILE] : " + inputFile);

		RTPCEngine en = new RTPCEngine();
		en.init();
		
		
		
		HipoDataSource reader = new HipoDataSource();	
		HipoDataSync writer = new HipoDataSync();
		reader.open(inputFile);
		writer.open(outputFile);
		System.out.println("starting " + starttime);
		while(reader.hasEvent()){	
			
			DataEvent event = reader.getNextEvent();			
			en.processDataEvent(event);
			writer.writeEvent(event);
		}
		writer.close();
		System.out.println("finished " + (System.nanoTime() - starttime)*Math.pow(10,-9));
	}
}
