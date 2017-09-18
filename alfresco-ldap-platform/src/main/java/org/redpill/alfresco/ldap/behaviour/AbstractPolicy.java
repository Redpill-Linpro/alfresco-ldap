package org.redpill.alfresco.ldap.behaviour;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractPolicy implements InitializingBean {

  protected NodeService nodeService;

  protected PolicyComponent policyComponent;

  protected BehaviourFilter behaviourFilter;

  protected LockService lockService;

  public void setPolicyComponent(final PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public void setNodeService(final NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setBehaviourFilter(final BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }

  public void setLockService(final LockService lockService) {
    this.lockService = lockService;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(nodeService);
    Assert.notNull(policyComponent);
    Assert.notNull(behaviourFilter);
    Assert.notNull(lockService, "You must provide an instance of the LockService.");
  }

  protected boolean shouldSkipPolicy(final NodeRef nodeRef) {
    // if the node does not exist, exit
    if (nodeRef == null || !nodeService.exists(nodeRef)) {
      return true;
    }

    // don't do this for working copies
    if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)) {
      return true;
    }

    // if it's not the spaces store, exit
    if (!StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(nodeRef.getStoreRef())) {
      return true;
    }

    // if it's anything but locked, don't do anything
    if (lockService.getLockStatus(nodeRef) != LockStatus.NO_LOCK) {
      return true;
    }

    return false;
  }

}
