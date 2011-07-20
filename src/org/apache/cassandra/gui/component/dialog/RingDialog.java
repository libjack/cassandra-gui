package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.node.NodeInfo;
import org.apache.cassandra.node.RingNode;
import org.apache.cassandra.node.Tpstats;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.RotatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;

public class RingDialog extends JDialog {
    private static final long serialVersionUID = 1543749033698969116L;

    private static final int NODE_STATUS_UP = 1;
    private static final int NODE_STATUS_DOWN = 2;
    private static final int NODE_STATUS_UNKNOWN = 3;

    private Client client;

    public RingDialog(Client client) {
        this.client = client;

        JScrollPane scrollPane = new JScrollPane(setupControls());

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Ring");
        setLocationRelativeTo(null);
        setModal(true);
    }

    private VisualizationViewer setupControls() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final RingNode ringNode = client.listRing();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        final Map<Token, String> rangeMap = ringNode.getRangeMap();
        final List<Token> ranges = ringNode.getRanges();
        final List<String> liveNodes = ringNode.getLiveNodes();
        final List<String> deadNodes = ringNode.getDeadNodes();
        final Map<String, String> loadMap = ringNode.getLoadMap();

        final Map<Vertex, Integer> statusMap = new HashMap<Vertex, Integer>();
        final Map<Vertex, String> endpointMap = new HashMap<Vertex, String>();

        final UndirectedSparseGraph graph = new UndirectedSparseGraph();
        final StringLabeller stringLabeller = StringLabeller.getLabeller(graph);

        final Vertex[] vertices = new Vertex[rangeMap.size()];

        int count = 0;
        for (Token range : ranges) {
//            List<String> endpoints = rangeMap.get(range);
            String endpoints = rangeMap.get(range);
//            String primaryEndpoint = endpoints.get(0);
            String primaryEndpoint = rangeMap.get(range);
            String load = loadMap.containsKey(primaryEndpoint) ? loadMap.get(primaryEndpoint) : "?";
            String label = "<html>" +
                           "Address: " + primaryEndpoint + "<br/>" +
                           "Load: " + load + "<br/>" +
                           "Range: " + range.toString() +
                           "</html>";

            Vertex v = graph.addVertex(new UndirectedSparseVertex());
            vertices[count] = v;
            try {
                stringLabeller.setLabel(v, label);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                e.printStackTrace();
            }

            statusMap.put(v, liveNodes.contains(primaryEndpoint) ? NODE_STATUS_UP
                                    : deadNodes.contains(primaryEndpoint) ? NODE_STATUS_DOWN
                                    : NODE_STATUS_UNKNOWN);
            endpointMap.put(v, primaryEndpoint);

            count++;
        }

        if (vertices.length > 1) {
            for (int i = 0; i < vertices.length; i++) {
                int index = 0;
                if (i+1 != vertices.length) {
                    index = i + 1;
                }
    
                graph.addEdge(new UndirectedSparseEdge(vertices[i], vertices[index]));
            }
        }

        final Layout layout = new CircleLayout(graph);
        final PluggableRenderer renderer = new PluggableRenderer();

        renderer.setVertexStringer(new VertexStringer() {
            @Override
            public String getLabel(ArchetypeVertex v) {
                return stringLabeller.getLabel(v);
            }
        });

        renderer.setVertexPaintFunction(new VertexPaintFunction() {
            @Override
            public Paint getFillPaint(Vertex v) {
                Color c = Color.YELLOW;
                switch (statusMap.get(v)) {
                case NODE_STATUS_UP:
                    c = Color.GREEN;
                    break;
                case NODE_STATUS_DOWN:
                    c = Color.RED;
                    break;
                }

                return c;
            }

            @Override
            public Paint getDrawPaint(Vertex v) {
                Color c = Color.YELLOW;
                switch (statusMap.get(v)) {
                case NODE_STATUS_UP:
                    c = Color.GREEN;
                    break;
                case NODE_STATUS_DOWN:
                    c = Color.RED;
                    break;
                }

                return c;
            }
        });

        PluggableGraphMouse gm = new PluggableGraphMouse();
        gm.add(new PickingGraphMousePlugin());
        gm.add(new RotatingGraphMousePlugin());
        gm.add(new TranslatingGraphMousePlugin());
        gm.add(new ScalingGraphMousePlugin(new LayoutScalingControl(), 0));
        gm.add(new AbstractPopupGraphMousePlugin() {
            @Override
            protected void handlePopup(MouseEvent e) {
                final VisualizationViewer vv = (VisualizationViewer) e.getSource();
                final Point2D ivp = vv.inverseViewTransform(e.getPoint());
                PickSupport pickSupport = vv.getPickSupport();
                final Vertex vertex = pickSupport.getVertex(ivp.getX(), ivp.getY());

                if(pickSupport != null) {
                    JPopupMenu popup = new JPopupMenu();
                    if(vertex != null) {
                        if (statusMap.get(vertex) == NODE_STATUS_UP) {
                            popup.add(new AbstractAction("info") {
                                private static final long serialVersionUID = -6992429747383272830L;

                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    try {
                                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                        NodeInfo ni = client.getNodeInfo(endpointMap.get(vertex));
                                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                                        NodeInfoDialog nid = new NodeInfoDialog(ni);
                                        nid.setVisible(true);
                                    } catch (Exception e) {
                                        JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            });

                            popup.add(new AbstractAction("tpstats") {
                                private static final long serialVersionUID = 6511117264115071716L;

                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    try {
                                        String endpoint = endpointMap.get(vertex);

                                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                        List<Tpstats> l = client.getTpstats(endpoint);
                                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                                        TpstatsDialog td = new TpstatsDialog(endpoint, l);
                                        td.setVisible(true);
                                    } catch (Exception e) {
                                        JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }

                    if(popup.getComponentCount() > 0) {
                        popup.show(vv, e.getX(), e.getY());
                    }
                }
            }
        });

        final VisualizationViewer viewer = new VisualizationViewer(layout, renderer);
        viewer.setGraphMouse(gm);
        viewer.setPickSupport(new ShapePickSupport(viewer, viewer, renderer, 2));

        return viewer;
    }
}
