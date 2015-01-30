package com.mercury.ticket.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercury.common.db.HibernateDao;
import com.mercury.ticket.persistence.model.Ticket;
import com.mercury.ticket.persistence.model.Transaction;
import com.mercury.ticket.util.TransactionQueue;


@Service
@Transactional
public class TicketService {
	@Autowired
	@Qualifier("ticketDao")
	private HibernateDao<Ticket,Integer> hd;
	/*
	public HibernateDao<Ticket, Integer> getHd() {
		return hd;
	}

	public void setHd(HibernateDao<Ticket, Integer> hd) {
		this.hd = hd;
	}
	*/
	@Autowired
	@Qualifier("transactionDao")
	private HibernateDao<Transaction,Integer> hd2;
	
	public List<Ticket> getAllTickets(){
		return hd.findAll();
	}
	public String updateTicket(Ticket ticket){
		hd.save(ticket);
		return "Succeed";
	}
	public List<Ticket> userFindTicket(String dep,String des,String depdate,int quantity){
		List<Ticket> l=null;
		
		l=hd.findAllBy3("dep", dep, "des", des, "depdate", depdate);
		for(Ticket t:l){
			if(t.getAvailable()<quantity) l.remove(t);
		}
		return l;
	}
	
	public Transaction buyTicketEnqueue(int userid,int tid,int quantity,int method){
		//another way to get the current time only
		/* 
		Calendar cal = Calendar.getInstance();
    	cal.getTime();
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    	System.out.println( sdf.format(cal.getTime()) );
		*/
		Date date = new Date();
		SimpleDateFormat ft = 
			      new SimpleDateFormat ("yyyyMMddhhmmss");
		
		
		Transaction trans=new Transaction();
		
		trans.setTid(tid);
		trans.setUserid(userid);
		trans.setQuantity(quantity);
		trans.setMethod(method);
		trans.setStatus("p");
		trans.setTime(ft.format(date));
		TransactionQueue.add(trans);
		return trans;
	}
	
	public String buyTicketDequeue(Transaction trans){
		String s=null;
		int quantity=trans.getQuantity();
		Ticket t=hd.findBy("tid", trans.getTid());
		int available=t.getAvailable();
		if(quantity<=available){
			trans.setStatus("s");
			t.setAvailable(available-quantity);
			t.setSold(t.getSold()+quantity);
			hd.save(t);
			hd2.save(trans);
			s=trans.getUserid()+" "+trans.getTid()+trans.getStatus()+"Succeed";
		}
		return s;
	}
	
	public List<Transaction> findHistory(int userid){
		return hd2.findAllBy("userid", userid);
	}
	
	public String removeTicket(int transactionid,int tid,int quantity){
		Transaction trans=hd2.findBy("transactionid", transactionid);
		Ticket t=hd.findBy("tid", tid);
		t.setSold(t.getSold()-quantity);
		t.setAvailable(t.getAvailable()+quantity);
		hd.save(t);
		Transaction newtrans=new Transaction();
		newtrans.setMethod(trans.getMethod());
		newtrans.setQuantity(quantity);
		newtrans.setStatus("r");
		newtrans.setTid(tid);
		newtrans.setUserid(trans.getUserid());
		hd2.save(newtrans);
		
		return "ticket has been returned";
	}
}