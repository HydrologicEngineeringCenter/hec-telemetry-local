package mil.army.usace.hec.opentelemetry.telemetryvue.gui.model.actions;

import com.google.common.flogger.FluentLogger;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.actions.PickDatabaseAction;
import rma.services.annotations.ServiceProvider;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ServiceProvider(service = PickDatabaseAction.class)
public class JFileChooserPickFileAction implements PickDatabaseAction<Path> {

    private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

    public JFileChooserPickFileAction() {

    }

    @Override
    public Class<Path> supportedDatabaseObject() {
        return Path.class;
    }

    @Override
    public Path pickDatabase(Map<String, Object> additionalParameters) {
        Object obj = additionalParameters.get("allowedFileTypes");
        Map<String, String> allowedFileTypes = null;
        if(obj instanceof Map) {
            allowedFileTypes = (Map<String, String>) obj;
        }
        Component parentComponent = (Component) additionalParameters.get("parentComponent");
        if(SwingUtilities.isEventDispatchThread()) {
            return pickFile(allowedFileTypes, parentComponent);
        } else {
            Map<String, String> finalAllowedFileTypes = allowedFileTypes;
            List<Path> path = new ArrayList<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    Path file = pickFile(finalAllowedFileTypes, parentComponent);
                    if(file != null) {
                        path.add(file);
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                LOGGER.atSevere().withCause(e).log("Error picking file");
            }
            return path.stream()
                    .findFirst()
                    .orElse(null);
        }
    }

    public Path pickFile(Map<String, String> allowedFileTypes, Component parentComponent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if(!(allowedFileTypes == null || allowedFileTypes.isEmpty())) {
            fileChooser.setAcceptAllFileFilterUsed(false);
            allowedFileTypes.forEach((description, extension) -> {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extension);
                fileChooser.addChoosableFileFilter(filter);
            });
        }
        int status = fileChooser.showOpenDialog(parentComponent);
        if(status == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().toPath();
        }
        return null;
    }
}
