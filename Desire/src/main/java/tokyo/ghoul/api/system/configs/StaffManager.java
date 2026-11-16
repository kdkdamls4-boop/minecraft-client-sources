package tokyo.ghoul.api.system.configs;

import lombok.Getter;
import tokyo.ghoul.api.system.files.AbstractFile;

public class StaffManager extends AbstractFile {
    @Getter private static final StaffManager instance = new StaffManager();

    @Override
    public String fileName() {
        return "staffs";
    }
}
