package heim.tasks

import com.google.common.base.Objects

/**
 * Created by glopes on 09-10-2015.
 */
abstract class AbstractTask implements Task {

    final UUID uuid

    AbstractTask() {
        this.uuid = UUID.randomUUID()
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("class", getClass().simpleName)
                .add("uuid", uuid)
                .toString();
    }
}
