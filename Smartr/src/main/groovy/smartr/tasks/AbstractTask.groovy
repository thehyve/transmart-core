package smartr.tasks

import com.google.common.base.Objects

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
