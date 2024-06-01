package customer.batchimportcat.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
// import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
// import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import cds.gen.dataimportservice.BatchImportFile;
import cds.gen.dataimportservice.BatchImportFile_;
import cds.gen.dataimportservice.DataImportService_;
import cds.gen.dataimportservice.ImplementedByClass;
import cds.gen.dataimportservice.ImplementedByClass_;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.Reflection;
import com.sap.cds.Result;
import com.sap.cds.ResultBuilder;
import com.sap.cds.ql.cqn.AnalysisResult;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;

import cds.gen.dataimportservice.ImportStructure_;
import customer.batchimportcat.utils.CheckDataVisitor;
import customer.batchimportcat.utils.ClassReflection;
import customer.batchimportcat.utils.UnmanagedReportUtils;
import cds.gen.dataimportservice.ImportStructure;

import com.sap.cds.services.*;;

@Component
@ServiceName(DataImportService_.CDS_NAME)
public class DataImportServiceHandler implements EventHandler {
  @Autowired
  @Qualifier("asyncJobLauncher")
  JobLauncher jobLauncher;

  @Autowired
  @Qualifier("batchImportJob")
  Job job;

  @Autowired
  CdsModel model;

  @On(event = CqnService.EVENT_CREATE, entity = BatchImportFile_.CDS_NAME)
  public void callBatchJob(Stream<BatchImportFile> batchImportFiles)
  // throws JobExecutionAlreadyRunningException,
  // JobRestartException, JobInstanceAlreadyCompleteException,
  // JobParametersInvalidException
  {

    batchImportFiles.forEach((file) -> {
      // jobLauncher.run(job, new JobParameters());
      // parse fileUUID into Job Parameters
      Map<String, JobParameter<?>> parMap = new HashMap<>();
      parMap.put("fileUUID", new JobParameter<>(file.getId(), String.class));

      try {
        JobExecution jobExecution = jobLauncher.run(job, new JobParameters(parMap));
        file.setJobName(jobExecution.getJobId().toString());

      } catch (Exception e) {
        // TODO: handle exception
        System.out.println(e.getMessage());
      }

    });
  }

  @On(event = CqnService.EVENT_READ, entity = ImportStructure_.CDS_NAME)
  public void getAllImportStructureVH(CdsReadEventContext context) {
    // Empty list of entity
    List<ImportStructure> importStructures = new ArrayList<ImportStructure>();

    // Get CqnSelect
    CqnSelect select = context.getCqn();

    // Get structure by annotation batchdataimport
    Stream<CdsStructuredType> structures = model.structuredTypes()
        .filter(com.sap.cds.reflect.CdsAnnotatable.byAnnotation("batchdataimport"));

    structures.forEach((structure) -> {
      // create entity
      ImportStructure importStructure = ImportStructure.create();

      importStructure.setName(structure.getName());
      importStructure.setDescription(structure.getAnnotationValue("title", null));

      CheckDataVisitor checkDataVisitor = new CheckDataVisitor(importStructure);
      try {
        CqnPredicate cqnPredicate = select.where().get();
        cqnPredicate.accept(checkDataVisitor);
        if (checkDataVisitor.matches()) {
          importStructures.add(importStructure);
        }
      } catch (Exception e) {
        // No where conditions
        importStructures.add(importStructure);
      }

    });

    // sort
    UnmanagedReportUtils.sort(select.orderBy(), importStructures);

    long inlineCount = importStructures.size();

    List<? extends Map<String, ?>> resultsPaging = UnmanagedReportUtils.getTopSkip(select.top(),
        select.skip(), importStructures);
    // set result by ResultBuilder
    Result result = ResultBuilder.selectedRows(resultsPaging).inlineCount(inlineCount).result();
    context.setResult(result);
  }

  @On(event = CqnService.EVENT_READ, entity = ImplementedByClass_.CDS_NAME)
  public void getAllImplementedByClass(CdsReadEventContext context) throws IOException {
    List<Map<String, Object>> implementedByClasses = new ArrayList<>();
    List<Map<String, ? extends Object>> implementedByClasseResult = new ArrayList<>();

    // Get CqnSelect
    CqnSelect select = context.getCqn();
    // ReflectionUtils.
    implementedByClasses = ClassReflection.getClassbyInterface(ItemWriter.class);

    implementedByClasses.forEach((clazz) -> {
      CheckDataVisitor checkDataVisitor = new CheckDataVisitor(clazz);
      try {
        CqnPredicate cqnPredicate = select.where().get();
        cqnPredicate.accept(checkDataVisitor);
        if (checkDataVisitor.matches()) {
          implementedByClasseResult.add(clazz); 
        }
      } catch (Exception e) {
        // No where conditions
        implementedByClasseResult.add(clazz);
      }
    });

    // sort
    UnmanagedReportUtils.sort(select.orderBy(), implementedByClasseResult);

    long inlineCount = implementedByClasseResult.size();

    List<? extends Map<String, ?>> resultsPaging = UnmanagedReportUtils.getTopSkip(select.top(),
        select.skip(), implementedByClasseResult);
    // set result by ResultBuilder
    Result result = ResultBuilder.selectedRows(resultsPaging).inlineCount(inlineCount).result();
    context.setResult(result);

  }

}
