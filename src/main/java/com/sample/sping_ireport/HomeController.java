package com.sample.sping_ireport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.sample.sping_ireport.model.Area;
import com.sample.sping_ireport.model.JavaBeanPerson;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);
		
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		
		String formattedDate = dateFormat.format(date);
		
		model.addAttribute("serverTime", formattedDate );
		
		return "home";
	}
	
	@RequestMapping(value = "/report", method = RequestMethod.GET)
	public String report(Model model) {
		// 报表数据源
		JRDataSource jrDataSource = new JRBeanCollectionDataSource(JavaBeanPerson.getList());
		
		// 动态指定报表模板url
		model.addAttribute("url", "/WEB-INF/jasper/spring_report.jasper");
		model.addAttribute("format", "pdf"); // 报表格式
		model.addAttribute("jrMainDataSource", jrDataSource);
		
		return "reportView"; // 对应jasper-views.xml中的bean id
	}
	
	
	@RequestMapping(value = "/report2", method = RequestMethod.GET)
	public String report2(Model model,HttpServletRequest request) {
		// 报表数据源
		List<Area> list = new ArrayList<Area>();
		Area a = new Area();
		a.setCode("010");
		a.setName("北京");
		a.setSimplename("j");
		list.add(a);
		JRDataSource jrDataSource = new JRBeanCollectionDataSource(list);
		
		// 动态指定报表模板url
		model.addAttribute("url", "/WEB-INF/jasper/area.jasper");
		model.addAttribute("format", "pdf"); // 报表格式
		model.addAttribute("jrMainDataSource", jrDataSource);
		return "reportView"; // 对应jasper-views.xml中的bean id
	}
	@RequestMapping(value = "/report3", method = RequestMethod.GET)
	public String report3(HttpServletRequest request,HttpServletResponse response){
		ReportUtils utils = new ReportUtils(request,response);
		List<Area> list = new ArrayList<Area>();
		response.setCharacterEncoding("text/html;charset=GB2312");
		Area a = new Area();
		a.setCode("010");
		a.setName("北京");
		a.setSimplename("j");
		list.add(a);
		Map params = new HashMap();
		try {
			utils.servletExportPDF( "c:/area.jasper", params, list, "aa.pdf");
			String url = request.getContextPath()+"/servlets/image?image=";
			System.out.println(url);
			utils.servletExportHTML("c:/area.jasper", params, list,url );
		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping(value = "/printed")
	public void printed(HttpServletRequest request, HttpServletResponse response)
	    throws IOException, ServletException, DocumentException{

	    ServletContext context = request.getSession().getServletContext();
	    try
	    {
	        //获得模板
	        String reportFileName = context.getRealPath("/WEB-INF/jasper/area.jasper");
	        File reportFile = new File(reportFileName);
	        if (!reportFile.exists())
	            throw new JRRuntimeException("文件 demo-report.jasper不能找到.");

	        //设置模板中需要的参数
	        Map<String, Object> parameters = new HashMap<String, Object>();
	        parameters.put("parameters1", "K12013102900001");
	        parameters.put("parameters2", "中文");
	        parameters.put("logoUrl", context.getRealPath("/WEB-INF/reports/demo.jpg"));

	        //读取模板文件
	        FileInputStream fileInputStream = new FileInputStream(reportFile);
	     // 报表数据源
			List<Area> list = new ArrayList<Area>();
			Area a = new Area();
			a.setCode("010");
			a.setName("北京");
			a.setSimplename("j");
			list.add(a);
			JRDataSource jrDataSource = new JRBeanCollectionDataSource(list);
	        //将模板转成PDF
	        byte[] pdfStream = JasperRunManager.runReportToPdf(fileInputStream, parameters, jrDataSource);

	        //输出流
	        PdfReader reader = new PdfReader(pdfStream);

	        ByteArrayOutputStream bos = new ByteArrayOutputStream(pdfStream.length);
	        try {
	                //给pdf加上脚本实现自动打印
	                StringBuffer script = new StringBuffer();
	                script.append("this.print({bUI:false,bSilent:true,bShrinkToFit:false});");

	                PdfStamper stamp = new PdfStamper(reader, bos);
	                stamp.setViewerPreferences(PdfWriter.HideMenubar | PdfWriter.HideToolbar | PdfWriter.HideWindowUI);
	                stamp.addJavaScript(script.toString());
	                stamp.close();

	            } catch (DocumentException e) {
	                logger.error(e.getMessage(), e.getCause());
	            }

	        //输出pdf
	        byte[] bytes = bos.toByteArray();
	        if (bytes != null && bytes.length > 0){
	             response.setContentType("application/pdf");
	             response.setContentLength(bytes.length);
	             ServletOutputStream ouputStream = response.getOutputStream();
	             try{
	                 ouputStream.write(bytes, 0, bytes.length);
	                 ouputStream.flush();
	             }
	             finally{
	                 if (ouputStream != null){
	                     try
	                        {
	                        ouputStream.close();
	                     }
	                    catch (IOException ex)
	                     {
	                    }
	                 }
	             }
	        }

	    }
	    catch (JRException e)
	    {
	        e.printStackTrace();
	    }
	}
}
