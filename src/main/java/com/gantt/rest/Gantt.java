package com.gantt.rest;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("rest/gantt")
@Produces({ MediaType.APPLICATION_JSON })
public class Gantt {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<TaskObject> getGanttChartData(@QueryParam("piid") String piid, @QueryParam("showall") Boolean showall) {
        // This will be the response back. It will be a JSON object the gantt chart library can use to render diagram
        ArrayList<TaskObject> taskObjects = new ArrayList<>();

        // This is the previous task map which must first be built before we build the response
        HashMap<String, ArrayList<String>> prevTasks = new HashMap<>();

        // Retrieve engine and process model
        ProcessEngine pe = ProcessEngines.getDefaultProcessEngine();
        //InputStream is = pe.getRepositoryService().getProcessModel("SampleForCockpit:2:990f2667-eb2b-11eb-96f4-0a0027000005");
        InputStream is = pe.getRepositoryService().getProcessModel("SampleForCockpit:1:185541c6-fb83-11eb-ac98-0a0027000005");
        // Create model from which we'll retrieve what we need
        BpmnModelInstance modelInstance = Bpmn.readModelFromStream(is);

        // Get all tasks from the model and prepare a way to iterate over them
        Collection<Task> tasks = modelInstance.getModelElementsByType(Task.class);
        Iterator<Task> iter = tasks.iterator();

        // Iterate over the tasks retrieving the previous task(s) for each task
        while(iter.hasNext()) {
            // Get current task
            Task task = iter.next();

            String elementType = task.getElementType().getInstanceType().getSimpleName();
            if (showall) {
                switch (elementType) {
                    case ("StartEvent"):
                    case ("ParallelGateway"):
                    case ("ExclusiveGateway"):
                    case ("IntermediateCatchEvent"):
                        // ignore it
                        break;

                    case ("UserTask"):
                    case ("ServiceTask"):
                    case ("ScriptTask"):
                    case ("ManualTask"):
                        // process it
                        // Create an array of the previous tasks
                        ArrayList<String> previousTaskNames = new ArrayList<>();

                        // Get previous nodes of current task and prepare to iterate over them
                        Query<FlowNode> prevNodes = task.getPreviousNodes();
                        List<FlowNode> prevFns = prevNodes.list();
                        Iterator<FlowNode> prevFnIter = prevFns.iterator();

                        // Task might have more than one previous task. Need to iterate through them
                        while(prevFnIter.hasNext()) {
                            FlowNode prevFn = prevFnIter.next();
                            previousTaskNames = findPreviousTask(prevFn, previousTaskNames, modelInstance, showall);

                            // This is just for testing
                            Iterator prevTnIter = previousTaskNames.iterator();
                            while(prevTnIter.hasNext()) {
                                System.out.println("Task "+task.getName()+ " previous task is " + prevTnIter.next());
                            }

                            // Put in prevTasks map
                            prevTasks.put(task.getName(), previousTaskNames);
                            //System.out.println("Task "+ task.getName()+" prev nodes "+ prevFn.getElementType().getTypeName());
                        }
                        break;

                    default:
                        System.out.println("Task ignored, type is " + elementType);
                        break;

                }
            } else {
                switch (elementType) {
                    case ("StartEvent"):
                    case ("ServiceTask"):
                    case ("ScriptTask"):
                    case ("ManualTask"):
                    case ("ParallelGateway"):
                    case ("ExclusiveGateway"):
                    case ("IntermediateCatchEvent"):
                        // ignore it
                        break;

                    case ("UserTask"):
                        // process it
                        // Create an array of the previous tasks
                        ArrayList<String> previousTaskNames = new ArrayList<>();

                        // Get previous nodes of current task and prepare to iterate over them
                        Query<FlowNode> prevNodes = task.getPreviousNodes();
                        List<FlowNode> prevFns = prevNodes.list();
                        Iterator<FlowNode> prevFnIter = prevFns.iterator();

                        // Task might have more than one previous task. Need to iterate through them
                        while(prevFnIter.hasNext()) {
                            FlowNode prevFn = prevFnIter.next();
                            previousTaskNames = findPreviousTask(prevFn, previousTaskNames, modelInstance, showall);

                            // This is just for testing
                            Iterator prevTnIter = previousTaskNames.iterator();
                            while(prevTnIter.hasNext()) {
                                System.out.println("Task "+task.getName()+ " previous task is " + prevTnIter.next());
                            }

                            // Put in prevTasks map
                            prevTasks.put(task.getName(), previousTaskNames);
                            //System.out.println("Task "+ task.getName()+" prev nodes "+ prevFn.getElementType().getTypeName());
                        }
                        break;

                    default:
                        System.out.println("Task ignored, type is " + elementType);
                        break;

                }
            }
        }

        // Now that we have the previous task info we can retrieve the audit info
        HistoricActivityInstanceQuery history = pe.getHistoryService().createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().processInstanceId(piid);

        // Get audit trail and create map for gantt chart to consume
        Iterator historyIter = history.list().iterator();
        while(historyIter.hasNext()){
            HistoricActivityInstance hai = (HistoricActivityInstance) historyIter.next();
            ArrayList<String> deps = prevTasks.get(hai.getActivityName());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date endTime = new Date();
            if(hai.getEndTime() != null) {
                endTime = hai.getEndTime();
            }
            if(hai.getActivityName() != null) {
                TaskObject taskObject = new TaskObject(hai.getActivityName(), hai.getActivityName(), sdf.format(hai.getStartTime()), sdf.format(endTime), 100, deps);

                String elementType = hai.getActivityType();
                System.out.println("Activity Type is "+ elementType);
                if (showall) {
                    switch (elementType) {
                        case ("startEvent"):
                        case ("parallelGateway"):
                        case ("exclusiveGateway"):
                        case ("intermediateCatchEvent"):
                            // ignore it
                            break;

                        case ("userTask"):
                        case ("serviceTask"):
                        case ("scriptTask"):
                        case ("manualTask"):
                            // add it
                            taskObjects.add(taskObject);
                            break;

                        default:
                            break;
                    }
                } else {
                    switch (elementType) {
                        case ("startEvent"):
                        case ("serviceTask"):
                        case ("scriptTask"):
                        case ("manualTask"):
                        case ("parallelGateway"):
                        case ("exclusiveGateway"):
                        case ("intermediateCatchEvent"):
                            // ignore it
                            break;

                        case ("userTask"):
                            // add it
                            taskObjects.add(taskObject);
                            break;

                        default:
                            break;
                    }
                }
            }

        }

        //System.out.println("Size is "+history.list().size());

        return taskObjects;
    }

    private ArrayList<String> findPreviousTask(FlowNode fn, ArrayList<String> taskNames, BpmnModelInstance modelInstance, Boolean showall) {
        //System.out.println("showall is "+showall);
        //String elementType = fn.getElementType().getBaseType().getTypeName();
        String elementType = fn.getElementType().getInstanceType().getSimpleName();
        //System.out.println("element is "+fn.getElementType().getInstanceType().getSimpleName());
        if(showall) {
            switch (elementType) {
                case ("StartEvent"):
                case ("ParallelGateway"):
                case ("ExclusiveGateway"):
                case ("IntermediateCatchEvent"):
                    // Since these elements are irrelevant in a Gantt diagram we need to find the next previous node until we get to a task
                    // Start events are ignored. May need to add more logic for inter catch events
                        Query<FlowNode> prevNodes = fn.getPreviousNodes();
                        List<FlowNode> fns = prevNodes.list();
                        Iterator iter = fns.iterator();
                        while (iter.hasNext()) {
                            taskNames = findPreviousTask((FlowNode) iter.next(), taskNames, modelInstance, showall);
                        }
                    break;

                case ("UserTask"):
                case ("ServiceTask"):
                case ("ScriptTask"):
                case ("ManualTask"):
                    taskNames.add(fn.getName());
                    break;

                default:
                    System.out.println("Node ignored, type is " + elementType);
                    break;
            }
        } else {
            switch (elementType) {
                case ("StartEvent"):
                case ("ServiceTask"):
                case ("ScriptTask"):
                case ("ManualTask"):
                case ("ParallelGateway"):
                case ("ExclusiveGateway"):
                case ("IntermediateCatchEvent"):
                    // Since these elements are irrelevant in a Gantt diagram we need to find the next previous node until we get to a task
                    // Start events are ignored. May need to add more logic for inter catch events

                        Query<FlowNode> prevNodes = fn.getPreviousNodes();
                        List<FlowNode> fns = prevNodes.list();
                        Iterator iter = fns.iterator();
                        while (iter.hasNext()) {
                            taskNames = findPreviousTask((FlowNode) iter.next(), taskNames, modelInstance, showall);
                        }
                    break;

                case ("UserTask"):
                    taskNames.add(fn.getName());
                    break;

                default:
                    System.out.println("Node ignored, type is " + elementType);
                    break;
            }
        }
        return taskNames;
    }
}