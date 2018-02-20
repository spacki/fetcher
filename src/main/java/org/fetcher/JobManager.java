package org.fetcher;

import org.fetcher.model.Job;
import org.fetcher.model.JobDAO;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.lifecycle.Managed;

public class JobManager implements Managed {

  ConcurrentHashMap<Integer, Fetcher> fetchers = new ConcurrentHashMap<>();
  private JobDAO jobDAO;

  public JobManager(JobDAO jobDAO) {
    this.jobDAO = jobDAO;
  }

  public Fetcher getById(int id) {
    return fetchers.get(id);
  }

  @UnitOfWork
  public Fetcher create(Job job) {
    jobDAO.presist(job);
    Fetcher f = new Fetcher(job);
    fetchers.put(job.getJobId(), f);
    return f;
  }

  @Override
  @UnitOfWork
  public void start() throws Exception {
    for (Job job : this.jobDAO.findAll()) {
      fetchers.put(job.getJobId(), new Fetcher(job));
    }
  }

  @Override
  public void stop() throws Exception {
    // TODO Auto-generated method stub
  }

  public Collection<Fetcher> getAll() {
    return fetchers.values();
  }

}
