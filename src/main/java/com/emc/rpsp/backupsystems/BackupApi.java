package com.emc.rpsp.backupsystems;

import com.emc.rpsp.core.service.impl.BaseServiceImpl;
import com.emc.rpsp.exceptions.RpspBackupSystemNotSetException;
import com.emc.rpsp.exceptions.RpspParamsException;
import com.emc.rpsp.fal.Client;
import com.emc.rpsp.vmwal.VSphereApi;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by morand3 on 1/3/2016.
 */
@Service
public class BackupApi extends BaseServiceImpl {
    private final Logger log = LoggerFactory.getLogger(BackupApi.class);
    @Autowired
    private BackupSystemsRepository repository;
    @Autowired
    private VmBackupRepository vmBackupRepo;

    public List<String> getBackupsList(BackupSystem system) {
        VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
        List<String> vms = vSphereApi.vmsInFolder(system.getBackupFolder());
        return vms;
    }

    public List<String> getBackupsList(String vmName) {
        VmBackup vm = repository.findVmByName(vmName);
        List<String> vms = getBackupsList(vm.getBackupSystem());
        return fetchVmBackupsList(vms, vmName);
    }

    public List<String> fetchVmBackupsList(List<String> backups, String vmName) {
        List<String> res = new LinkedList<>();
        for (String backup : backups) {
            if (backup.startsWith(vmName))
                res.add(backup);
        }
        return res;
    }

    public void addVmToBackupSchedule(String vmId, String vmName, String schedule) {
        List<BackupSystem> backupSystems = repository.findAll();
        if (backupSystems.isEmpty()) {
            throw new RpspBackupSystemNotSetException();
        }
        BackupSystem system = backupSystems.get(0);
        VmBackup backup = new VmBackup(system, vmId, vmName, schedule);
        vmBackupRepo.saveAndFlush(backup);
    }


    public void backupVm(String vmName) {
        VmBackup vm = repository.findVmByName(vmName);
        Client client = getClient();
        BackupImageAccessParams params = getImageAccessParams(client, vm.getVmId());
        client.enableLatestImageAccess(params.getClusterId(), params.getGroupId(), params.getCopyId());
//////////Wait until enabled..
        String vmDrTestName = params.getReplicaName();
        BackupSystem system = vm.getBackupSystem();
        VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
        try {
            vSphereApi.cloneVM(vmDrTestName, system.getBackupFolder(), currentBackupName(vmName), system.getBackupDatastore(), false);
        } catch (Exception e) {
            log.warn("Failed to backup/clone VM {}", vmName);
        }
    }

    public void enableAccessBackup(BackupSystem system, String backupName) throws Exception {
        if (null == system || null == backupName) {
            log.warn("Called enable access backup with no params");
            throw new RpspParamsException();
        }
        try {
            VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
            vSphereApi.cloneVM(backupName,
                system.getAccessBackupFolder(),
                accessBackupName(backupName),
                system.getAccessBackupDatastore(),
                true);
        } catch (Exception e) {
            log.warn("Failed to enable access to backup {}", backupName);
            throw e;
        }
    }

    public void disableAccessBackup(BackupSystem system, String backupName) throws Exception {
        if (null == system || null == backupName) {
            log.warn("Called enable access backup with no params");
            throw new RpspParamsException();
        }
        try {
            VSphereApi vSphereApi = new VSphereApi(system.getVcenterUrl(), system.getUsername(), system.getRealPassword());
            vSphereApi.removeVM(accessBackupName(backupName));
        } catch (Exception e) {
            log.warn("Failed to disable backup access VM {}", backupName);
            throw e;
        }
    }

    private BackupImageAccessParams getImageAccessParams(Client client, String productionVmId) {
        BackupImageAccessParams backupImageAccessParams = null;
//        Client client = getClient();
        Map<String, Object> replicaInfo = client.getReplicaInfoByProductionVmId(productionVmId);
        if (replicaInfo != null) {
            backupImageAccessParams = new BackupImageAccessParams(
                Long.parseLong(replicaInfo.get(BackupConsts.GROUP_ID).toString()),
                Long.parseLong(replicaInfo.get(BackupConsts.CLUSTER_ID).toString()),
                Integer.parseInt(replicaInfo.get(BackupConsts.COPY_ID).toString()),
                replicaInfo.get(BackupConsts.VM_NAME).toString());
        }
        return backupImageAccessParams;
    }


    private String accessBackupName(String backup) {
        return backup + "_restore";
    }

    private String currentBackupName(String vmName) {
        return vmName + DateFormatUtils.format(new Date(), "_yyyy-MM-dd");
    }

    public void disableBackupForVmId(String vmId) {
        VmBackup backup = vmBackupRepo.findByVmId(vmId);
        if (null != backup) {
            vmBackupRepo.delete(backup);
        }
    }
}