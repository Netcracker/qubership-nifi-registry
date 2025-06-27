package org.qubership.cloud.nifi.registry.security.authorization.database.model;

import org.apache.nifi.registry.security.authorization.Group;
import org.apache.nifi.registry.security.authorization.User;
import org.apache.nifi.registry.security.authorization.UserAndGroups;

import java.util.Set;

public class UserAndGroupsImpl implements UserAndGroups {

    private User user;
    private Set<Group> groups;

    public UserAndGroupsImpl(User user, Set<Group> groups) {
        this.user = user;
        this.groups = groups;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    @Override
    public User getUser() {
        return this.user;
    }

    @Override
    public Set<Group> getGroups() {
        return this.groups;
    }
}
