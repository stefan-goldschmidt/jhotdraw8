/*
 * @(#)TextEditingTool.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.tool;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.application.resources.Resources;
import org.jhotdraw8.draw.DrawingEditor;
import org.jhotdraw8.draw.DrawingView;
import org.jhotdraw8.draw.figure.Figure;
import org.jhotdraw8.draw.figure.TextEditableFigure;
import org.jhotdraw8.draw.handle.HandleType;
import org.jhotdraw8.draw.model.DrawingModel;

import static java.lang.Math.max;

/**
 * TextEditingTool.
 *
 * @author Werner Randelshofer
 */
public class TextEditingTool extends AbstractTool {


    private double defaultWidth = 100;
    private double defaultHeight = 100;
    private final @NonNull TextArea textArea = new TextArea();
    private @Nullable TextEditableFigure.TextEditorData editorData;

    /**
     * The rubber band.
     */
    private double x1, y1, x2, y2;

    /**
     * The minimum size of a created figure (in view coordinates.
     */
    private final double minSize = 2;

    public TextEditingTool(String id, Resources labels) {
        super(id, labels);
        node.setCursor(Cursor.CROSSHAIR);

        textArea.setWrapText(true);
        textArea.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                keyEvent.consume();
                stopEditing();
            }
        });
    }

    public double getDefaultHeight() {
        return defaultHeight;
    }

    public void setDefaultHeight(double defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public double getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(double defaultWidth) {
        this.defaultWidth = defaultWidth;
    }


    @Override
    protected void stopEditing() {
        if (editorData != null) {
            node.getChildren().remove(textArea);
            DrawingView drawingView = getDrawingView();
            if (drawingView != null) {
                DrawingModel model = drawingView.getModel();
                model.set(editorData.figure, editorData.textKey, textArea.getText());
            }
            editorData = null;
        }
    }

    @Override
    protected void onMousePressed(@NonNull MouseEvent event, @NonNull DrawingView view) {
        event.consume();
        if (editorData != null) {
            stopEditing();
            return;
        }

        x1 = event.getX();
        y1 = event.getY();

        Figure figure = view.findFigure(x1, y1, f -> f.isSelectable() && f instanceof TextEditableFigure);
        if (figure instanceof TextEditableFigure) {
            TextEditableFigure f = (TextEditableFigure) figure;
            TextEditableFigure.TextEditorData data = f.getTextEditorDataFor(f.worldToLocal(new Point2D(x1, y1)), view.findFigureNode(f, x1, y1));
            if (data != null) {
                startEditing(data, view);
            }
        }
    }


    private void startEditing(@NonNull TextEditableFigure.TextEditorData data, @NonNull DrawingView dv) {
        dv.getSelectedFigures().clear();
        dv.getEditor().setHandleType(HandleType.SELECT);
        dv.getSelectedFigures().add(data.figure);
        editorData = data;
        textArea.setManaged(false);
        node.getChildren().add(textArea);
        Bounds bounds = dv.worldToView(data.figure.localToWorld(data.boundsInLocal));
        textArea.resizeRelocate(bounds.getMinX(), bounds.getMinY(), max(80, max(textArea.getMinWidth(), bounds.getWidth())),
                max(40, max(textArea.getMinHeight(), bounds.getHeight())));
        textArea.setText(data.figure.get(editorData.textKey));
        textArea.requestFocus();
    }

    @Override
    protected void onMouseReleased(@NonNull MouseEvent event, @NonNull DrawingView dv) {
        if (editorData != null) {
            return;
        }
    }

    @Override
    protected void onMouseDragged(@NonNull MouseEvent event, @NonNull DrawingView dv) {
        event.consume();
        if (editorData != null) {
            return;
        }
    }

    @Override
    protected void onMouseMoved(@NonNull MouseEvent event, @NonNull DrawingView view) {
        if (editorData != null) {
            return;
        }
        Figure figure = view.findFigure(event.getX(), event.getY());
        if (figure instanceof TextEditableFigure) {
            TextEditableFigure f = (TextEditableFigure) figure;
            TextEditableFigure.TextEditorData data = f.getTextEditorDataFor(f.getWorldToLocal().transform(event.getX(), event.getY()),
                    view.findFigureNode(f, event.getX(), event.getY()));
            if (data != null) {
                node.setCursor(Cursor.TEXT);
                return;
            }
        }
        node.setCursor(Cursor.CROSSHAIR);
    }

    @Override
    protected void onMouseClicked(@NonNull MouseEvent event, @NonNull DrawingView dv) {
    }


    /**
     * This implementation is empty.
     */
    @Override
    public void activate(@NonNull DrawingEditor editor) {
        requestFocus();
        super.activate(editor);
    }

    @Override
    public @NonNull String getHelpText() {
        return "CreationTool"
                + "\n  Click on the drawing view. The tool will create a new figure with default size at the clicked location."
                + "\nOr:"
                + "\n  Press and drag the mouse over the drawing view to define the diagonal of a rectangle. The tool will create a new figure that fits into the rectangle.";
    }

}
