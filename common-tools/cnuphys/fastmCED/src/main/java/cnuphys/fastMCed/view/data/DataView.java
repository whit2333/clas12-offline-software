package cnuphys.fastMCed.view.data;


import org.jlab.clas.physics.PhysicsEvent;

import cnuphys.bCNU.util.PropertySupport;
import cnuphys.bCNU.view.BaseView;
import cnuphys.fastMCed.eventio.IPhysicsEventListener;
import cnuphys.fastMCed.eventio.PhysicsEventManager;

public class DataView extends BaseView implements
		IPhysicsEventListener {

	protected DataTable _dataTable;

	protected PhysicsEventManager _eventManager = PhysicsEventManager
			.getInstance();

	public DataView(String title, int detector) {
		super(PropertySupport.TITLE, title, PropertySupport.ICONIFIABLE, true,
				PropertySupport.TOOLBAR, false,
				PropertySupport.MAXIMIZABLE, true, PropertySupport.CLOSABLE, true,
				PropertySupport.RESIZABLE, true, PropertySupport.WIDTH, DataTableModel.getPreferredWidth(),
				PropertySupport.HEIGHT, 700, PropertySupport.LEFT, 700,
				PropertySupport.TOP, 100, PropertySupport.VISIBLE, true);

		_dataTable = new DataTable(detector);
		add(_dataTable.getScrollPane());

		// need to listen for events
		_eventManager.addPhysicsListener(this, 1);
	}

	@Override
	public void openedNewLundFile(String path) {
	}

	@Override
	public void newPhysicsEvent(PhysicsEvent event) {
		_dataTable.getDataModel().setData(_eventManager.getParticleHits());
	}

}