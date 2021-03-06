package com.alex.timetable.routesparser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;


public class HTMLParser {

	private boolean checkIsDigits(String string) {
        if (string == null || string.length() == 0) return false;

        int i = 0;
        char c;
        for (; i < string.length(); i++) {
            c = string.charAt(i);
            if (!(c >= '0' && c <= '9')) return false;
        }
        return true;
    }
	
	/**
	 * ���� ������� ���� �� �������� ����� ����� ����
	 * @param list
	 * @param mask
	 * @return
	 */
	private List<Node> getSubList(List<Node> list, String mask) {
		Iterator<Node> iterator = list.iterator();
		
		if(mask == null) mask = "";
		
		while(iterator.hasNext()) {
			Node tmp = iterator.next();
			if(mask.equals(tmp.nodeName())) return tmp.childNodes();
		}
		return null;
	}

	/**
	 * �������� ���� ���������� ���������� ��������.
	 * @param list
	 * @return
	 */
	private String getDateUpdateShedule(List<Node> list) {
		for(Node node: list) {
			String str = node.outerHtml();
			if(str.indexOf(">�<") > 0) {
				for(Node subNode: node.childNodes()) {
					if(subNode instanceof Element) {
						String data1 = ((Element) subNode).text().trim();
						if(data1.indexOf(" �") > 0) {
							data1 = data1.substring(data1.indexOf(" �") + 3, data1.indexOf("��"));
							return data1.trim();
						}
					}
				}
				break;
			}
		}
		return "";
	}

	private String getStationTimeFromNode(Node node) {
		String strTime = "";
		if(node != null && node.childNodeSize() > 0) strTime = node.childNodes().get(0).outerHtml().trim();
		if("&nbsp;".equals(strTime)) strTime = "";
		// ��������� ���� � �����, ����� ���������� �������� ���������
		if(strTime != null && strTime.length() > 0 && !strTime.startsWith("1") && !strTime.startsWith("2")) strTime = "0" + strTime;
		return strTime;
	}

	private boolean firstColumnHasNumber(Node hiNode) {
		if(hiNode == null || hiNode.childNodeSize() == 0) return false;
		for(Node rec: hiNode.childNodes()) {
			if(rec instanceof Element) {
				return checkIsDigits(((Element) rec).text());
			}
		}
		return false;
	}
	
	public EXTRoute parse(String htmlText, int routeId, String routeNum, int halfStationsNum, boolean isWorkDays) {
		Document doc = null;
		doc = Jsoup.parse(htmlText);
		if(doc == null) return null;
		Elements element = doc.getElementsByTag("tbody");

		EXTRoute route = new EXTRoute();
		route.setRouteId(routeId);
		route.setRouteNumber(routeNum);
		route.setWorkDays((isWorkDays?1:0));
		
		List<Node> list = element.get(0).childNodes(); 
		route.setLastUpdateDate(getDateUpdateShedule(list));
		
		List<EXTTmpRecordList> tmpList = new ArrayList<EXTTmpRecordList>();
		
		for(Node hiNode:list) {
			EXTTmpRecordList subList = new EXTTmpRecordList();
			
			if(hiNode instanceof Element) {
				int i = 0;
				if(firstColumnHasNumber(hiNode)) {
					for(Node subNode: hiNode.childNodes()) {
						if(subNode instanceof Element) {
							EXTTmpRecord rec = new EXTTmpRecord();
							rec.setId(i);
							rec.setValue(getStationTimeFromNode(subNode));
							subList.getItems().add(rec);
							i = i + 1;
						}
					}
				}
				if(subList != null && subList.getItems().size() > 0) tmpList.add(subList);
			}			
		}
		
		parseRightSides(tmpList, halfStationsNum);
		for(EXTTmpRecordList rec:tmpList) {
			EXTStop stop = new EXTStop();
			int i = 0;
			for(EXTTmpRecord subRec: rec.getItems()) {
				// ������������ ���������� ���� ������
				EXTStopTime stopTime = new EXTStopTime();
				stopTime.setFirstDirection((i < halfStationsNum?0:1));
				stopTime.setPosNum(i);
				stopTime.setStationTime(subRec.getValue());
				stop.getTimeList().add(stopTime);
				i++;
			}
			route.addStopIfTimeListNotNull(stop);
		}
		
		return route;
	}

	private void parseRightSides(List<EXTTmpRecordList> tmpList, int halfStationsNum) {
		if(tmpList == null || tmpList.size() <= 0) return;
		
		//List<EXTTmpRecordList> retList = new ArrayList<EXTTmpRecordList>();
		int i = 0;
		//int x = tmpList.get(0).getItems().size();
		while(i < tmpList.get(0).getItems().size()) {
			boolean flag = true;
			for(EXTTmpRecordList rec:tmpList) {
				if(((EXTTmpRecord) rec.getItems().get(i)).getValue().length() > 2) {
					flag = false;
					break;
				}
			}
		
			// ������� ��������� ������ ���
			if(flag) {
				for(EXTTmpRecordList rec:tmpList) rec.getItems().remove(i);
			} else i++;
		}
	}

}
