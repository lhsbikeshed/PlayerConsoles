package engineer.reactorsim;

import java.util.ArrayList;

public class ReactorManager {

	public static class ReactorCheck implements Comparable {

		String tag = "";

		String message = "";

		public boolean isOk = false;

		ReactorCheck(String tag, boolean isOk) {

			this.tag = tag;
			this.isOk = isOk;

		}

		@Override
		public int compareTo(Object arg0) {
			String comp = ((ReactorCheck) arg0).getTag();
			if (comp.equals(tag)) {
				return 0;
			} else {
				return -1;
			}
		}

		@Override
		public boolean equals(Object object) {
			String comp = ((ReactorCheck) object).getTag();
			if (comp.equals(tag)) {
				return true;
			} else {
				return false;
			}
		}

		public String getMessage() {
			return message;
		}

		public String getTag() {
			return tag;
		}

		public boolean isOk() {
			return isOk;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	ReactorModel model;
	ArrayList<ReactorCheck> problemList = new ArrayList<ReactorCheck>();
	ArrayList<ReactorCheck> tempList = new ArrayList<ReactorCheck>();
	
	public ReactorManager(ReactorModel model) {
		this.model = model;
	}

	public ArrayList<ReactorCheck> getProblemList() {
		return problemList;
	}

	public void tick() {
		// run through checks of the reactor model and suggest fixes
		tempList.clear();
		for (ReactorSystem sys : model.systems) {
			ArrayList<ReactorCheck> t = sys.checkForProblems();
			if (t != null) {
				tempList.addAll(t);
			}
		}

		for (int i = tempList.size() - 1; i >= 0; i--) {
			ReactorCheck p = tempList.get(i);
			if (problemList.contains(p) == false && p.isOk == false) {
				problemList.add(p);
			} else if (problemList.contains(p) == true && p.isOk == true) {
				problemList.remove(p);
			}
		}

	}

}
