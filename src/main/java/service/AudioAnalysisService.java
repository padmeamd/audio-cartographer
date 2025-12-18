package service;
import com.dakonst.audio_cartographer.dto.AnalysisResult;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class AudioAnalysisService {

    public AnalysisResult analyze(File audioFile){
        AnalysisResult r = new AnalysisResult();
        r.setFilename(audioFile.getName());
        return r;
    }

}
